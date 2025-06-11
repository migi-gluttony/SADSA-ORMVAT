import axios from 'axios';
import router from '../router';
import AuthService from './AuthService';

// Create axios instance
const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Helper functions for token management
const getToken = () => {
  return localStorage.getItem('token') || sessionStorage.getItem('token') || null;
};

// Request interceptor for adding the auth token
api.interceptors.request.use(
  (config) => {
    // Check if token is expired before making a request
    if (AuthService.isAuthenticated() && AuthService.isTokenExpired()) {
      // Token is expired, log out and redirect
      AuthService.logout();
      window.location.href = '/login';
      // Reject the request with a clear message
      return Promise.reject(new Error('Session expirée. Veuillez vous reconnecter.'));
    }

    const token = getToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor for handling common errors
api.interceptors.response.use(
  (response) => {
    // Return just the data by default
    return response.data;
  },
  (error) => {
    // Handle unauthorized errors (token expired, etc.)
    if (error.response && error.response.status === 401) {
      // Clear the token
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      sessionStorage.removeItem('token');
      sessionStorage.removeItem('user');

      // Redirect to login
      router.push('/login');
    }

    return Promise.reject(error.response?.data || error);
  }
);

/**
 * Service for making API requests
 */
const ApiService = {
  /**
   * Make a GET request
   * @param {string} endpoint - API endpoint
   * @param {object} params - Query parameters
   * @returns {Promise} - Promise with response data
   */
  async get(endpoint, params = {}) {
    try {
      return await api.get(endpoint, { params });
    } catch (error) {
      console.error(`GET ${endpoint} error:`, error);
      throw error;
    }
  },
  /**
   * Make a POST request
   * @param {string} endpoint - API endpoint
   * @param {object} data - Request payload
   * @param {object} config - Axios config (headers, etc.)
   * @returns {Promise} - Promise with response data
   */
  async post(endpoint, data = {}, config = {}) {
    try {
      return await api.post(endpoint, data, config);
    } catch (error) {
      console.error(`POST ${endpoint} error:`, error);
      throw error;
    }
  },
  /**
   * Make a PUT request
   * @param {string} endpoint - API endpoint
   * @param {object} data - Request payload
   * @param {object} config - Axios config (headers, etc.)
   * @returns {Promise} - Promise with response data
   */
  async put(endpoint, data = {}, config = {}) {
    try {
      return await api.put(endpoint, data, config);
    } catch (error) {
      console.error(`PUT ${endpoint} error:`, error);
      throw error;
    }
  },

  /**
   * Make a DELETE request
   * @param {string} endpoint - API endpoint
   * @returns {Promise} - Promise with response data
   */
  async delete(endpoint) {
    try {
      return await api.delete(endpoint);
    } catch (error) {
      console.error(`DELETE ${endpoint} error:`, error);
      throw error;
    }
  },

  /**
   * Upload files
   * @param {string} endpoint - API endpoint
   * @param {FormData} formData - Form data with files
   * @param {Function} progressCallback - Callback for upload progress
   * @returns {Promise} - Promise with response data
   */
  async uploadFiles(endpoint, formData, progressCallback = null) {
    try {
      const config = {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      };

      if (progressCallback) {
        config.onUploadProgress = (progressEvent) => {
          const percentCompleted = Math.round(
            (progressEvent.loaded * 100) / progressEvent.total
          );
          progressCallback(percentCompleted);
        };
      }

      return await api.post(endpoint, formData, config);
    } catch (error) {
      console.error(`UPLOAD to ${endpoint} error:`, error);
      throw error;
    }
  }
};

export default ApiService;