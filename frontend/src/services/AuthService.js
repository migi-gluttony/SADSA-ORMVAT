import ApiService from './ApiService';

/**
 * Service for handling authentication-related API calls
 * Tailored for SADSA ORMVAT API endpoints
 */
const AuthService = {
  /**
   * Clear all authentication data
   */
  clearAuthData() {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    sessionStorage.removeItem('token');
    sessionStorage.removeItem('user');
  },

  /**
   * Register a new user
   * @param {Object} userData - User registration data
   * @returns {Promise} - Promise with registration response
   */
  async register(userData) {
    try {
      // Clear any existing auth data before registration
      this.clearAuthData();
      
      const response = await ApiService.post('/auth/register', userData);
      
      // Store token if registration includes immediate login
      if (response.token) {
        const userInfo = this.parseToken(response.token);
        localStorage.setItem('token', response.token);
        if (userInfo) {
          localStorage.setItem('user', JSON.stringify(userInfo));
        }
      }
      
      window.dispatchEvent(new CustomEvent('auth-state-changed'));
      return response;
    } catch (error) {
      console.error('Registration error:', error);
      
      // Improve error handling
      if (error.message) {
        throw new Error(error.message);
      } else {
        throw new Error('Erreur lors de la création du compte');
      }
    }
  },

  /**
   * Parse JWT token to extract user information
   * @param {string} token - JWT token
   * @returns {Object} - User information from token
   */
  parseToken(token) {
    try {
      const base64Url = token.split('.')[1];
      const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
      const payload = JSON.parse(window.atob(base64));

      return {
        email: payload.sub,
        role: payload.role,
        userId: payload.userId,
        nom: payload.nom,
        prenom: payload.prenom,
        exp: payload.exp // Extract expiration timestamp
      };
    } catch (e) {
      console.error('Error parsing token:', e);
      return null;
    }
  },

  /**
   * Check if the current token is expired
   * @returns {boolean} - True if token is expired or invalid
   */
  isTokenExpired() {
    const token = this.getToken();

    if (!token) {
      return true; // No token means it's "expired"
    }

    try {
      const tokenData = this.parseToken(token);

      if (!tokenData || !tokenData.exp) {
        return true; // Invalid token or no expiration
      }

      // Add 30 seconds buffer to account for network delays
      const currentTime = Math.floor(Date.now() / 1000) + 30;

      return tokenData.exp < currentTime;
    } catch (error) {
      console.error('Error checking token expiration:', error);
      return true; // If we can't check, assume it's expired for safety
    }
  },

  /**
   * Check token and logout if expired
   */
  checkTokenAndLogout() {
    if (this.isAuthenticated() && this.isTokenExpired()) {
      console.log('Token expired, logging out');
      this.logout();
      window.location.href = '/login'; // Force redirect to login
    }
  },

  /**
   * Login a user with email and password
   * @param {string} email - User's email
   * @param {string} motDePasse - User's password
   * @param {boolean} rememberMe - Whether to remember the user
   * @returns {Promise} - Promise with authentication response
   */
  async login(email, motDePasse, rememberMe = false) {
    try {
      // Clear any existing auth data before login attempt
      this.clearAuthData();

      // Match the field names expected by the backend (LoginRequest.java)
      const response = await ApiService.post('/auth/login', {
        email,
        motDePasse
      });

      // Store token in localStorage/sessionStorage based on remember me option
      if (response.token) {
        // Parse token to extract user info
        const userInfo = this.parseToken(response.token);

        if (!userInfo) {
          throw new Error('Token invalide reçu du serveur');
        }

        // Store based on remember me preference
        if (rememberMe) {
          localStorage.setItem('token', response.token);
          localStorage.setItem('user', JSON.stringify(userInfo));
        } else {
          sessionStorage.setItem('token', response.token);
          sessionStorage.setItem('user', JSON.stringify(userInfo));
        }

        console.log('Login successful, user:', userInfo);
      } else {
        throw new Error('Aucun token reçu du serveur');
      }

      // Dispatch event when authentication state changes
      window.dispatchEvent(new CustomEvent('auth-state-changed'));

      return response;
    } catch (error) {
      console.error('Login error:', error);
      
      // Clear any auth data on login failure
      this.clearAuthData();
      
      // Improve error handling based on status codes and messages
      if (error.message.includes('401') || error.message.includes('incorrect')) {
        throw new Error('Email ou mot de passe incorrect');
      } else if (error.message.includes('403') || error.message.includes('activé')) {
        throw new Error('Votre compte n\'est pas encore activé');
      } else if (error.message) {
        throw new Error(error.message);
      } else {
        throw new Error('Erreur de connexion. Veuillez réessayer.');
      }
    }
  },

  /**
   * Logout the current user
   */
  logout() {
    console.log('Logging out user');
    this.clearAuthData();

    // Dispatch event when authentication state changes
    window.dispatchEvent(new CustomEvent('auth-state-changed'));
  },

  /**
   * Change user password
   * @param {string} oldPassword - Current password
   * @param {string} newPassword - New password
   * @returns {Promise} - Promise with change response
   */
  async changePassword(oldPassword, newPassword) {
    try {
      return await ApiService.post('/auth/change-password', {
        ancienMotDePasse: oldPassword,
        nouveauMotDePasse: newPassword
      });
    } catch (error) {
      console.error('Password change error:', error);
      
      // Improve error handling
      if (error.message.includes('400') || error.message.includes('incorrect')) {
        throw new Error('Mot de passe actuel incorrect');
      } else if (error.message) {
        throw new Error(error.message);
      } else {
        throw new Error('Erreur lors du changement de mot de passe');
      }
    }
  },

  /**
   * Check if user is authenticated
   * @returns {boolean} - True if authenticated
   */
  isAuthenticated() {
    const token = this.getToken();
    const isValid = token !== null && !this.isTokenExpired();
    
    if (!isValid && token) {
      // If token exists but is invalid/expired, clear it
      this.clearAuthData();
    }
    
    return isValid;
  },

  /**
   * Get the authentication token
   * @returns {string|null} - The token or null if not authenticated
   */
  getToken() {
    return localStorage.getItem('token') || sessionStorage.getItem('token') || null;
  },

  /**
   * Get the current user data
   * @returns {Object|null} - User data or null if not authenticated
   */
  getCurrentUser() {
    const userStr = localStorage.getItem('user') || sessionStorage.getItem('user');
    
    if (!userStr) {
      return null;
    }
    
    try {
      const user = JSON.parse(userStr);
      
      // Verify the token is still valid
      if (this.isTokenExpired()) {
        this.clearAuthData();
        return null;
      }
      
      return user;
    } catch (error) {
      console.error('Error parsing user data:', error);
      this.clearAuthData();
      return null;
    }
  },

  /**
   * Refresh authentication state - useful for checking after page reload
   */
  refreshAuthState() {
    const isAuth = this.isAuthenticated();
    
    if (!isAuth) {
      this.clearAuthData();
    }
    
    window.dispatchEvent(new CustomEvent('auth-state-changed'));
    return isAuth;
  }
};

export default AuthService;