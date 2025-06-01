<template>
    <aside v-if="isAuthenticated" class="app-sidebar" :class="{ collapsed: collapsed }">
        <div class="sidebar-content">
            <!-- Logo section with collapse toggle -->
            <div class="sidebar-header">
                <div class="sidebar-logo">
                    <!-- Full logo (visible when expanded) -->
                    <template v-if="!collapsed">
                        <router-link to="/" class="logo-link">
                            <div class="logo-container">
                                <div class="logo-icon">
                                    <img src="@/assets/logo/sadsa.svg" alt="sadsa" />
                                </div>
                            </div>
                        </router-link>
                        <button class="collapse-btn" @click="toggleSidebar" title="Réduire le menu">
                            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24"><g fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2"><path d="M4 6a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2zm5-2v16"/><path d="m15 10l-2 2l2 2"/></g></svg>
                        </button>
                    </template>

                    <!-- Small logo (visible when collapsed) -->
                    <template v-else>
                        <router-link to="/" class="logo-link collapsed-logo">
                            <div class="logo-icon-small">
                                <img src="@/assets/logo/sadsa.svg" alt="sadsa" />
                            </div>
                        </router-link>
                    </template>
                </div>

                <!-- Expand button (only visible when collapsed) -->
                <div v-if="collapsed" class="expand-btn-container">
                    <button class="expand-btn" @click="toggleSidebar" title="Étendre le menu">
<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24"><g fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2"><path d="M4 6a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2zm11-2v16"/><path d="m9 10l2 2l-2 2"/></g></svg>                    </button>
                </div>
            </div>

            <!-- User info section (when expanded) -->
            <div v-if="!collapsed && user" class="user-info">
                <div class="user-avatar">
                    <span class="user-initials">{{ getUserInitials(user) }}</span>
                </div>
                <div class="user-details">
                    <span class="user-name">{{ user.prenom }} {{ user.nom }}</span>
                    <span class="user-role">{{ getRoleDisplayName(user.role) }}</span>
                </div>
                <div class="user-status">
                    <div class="status-indicator"></div>
                </div>
            </div>

            <!-- Menu items -->
            <nav class="sidebar-nav">
                <div class="nav-section">
                    <div v-for="(item, index) in menuItems" :key="index" 
                         class="nav-item"
                         :class="{ active: isActiveRoute(item.route) }" 
                         @click="navigateTo(item.command)"
                         :title="collapsed ? item.label : ''">
                        <div class="nav-item-content">
                            <div class="nav-icon">
                                <i :class="item.icon"></i>
                            </div>
                            <span v-if="!collapsed" class="nav-label">{{ item.label }}</span>
                            <div v-if="item.badge && !collapsed" class="nav-badge">{{ item.badge }}</div>
                        </div>
                    </div>
                </div>
            </nav>

            <!-- Footer section -->
            <div class="sidebar-footer">
                <div class="footer-actions">
                    <div class="nav-item profile-item" 
                         @click="router.push('/profile')" 
                         :class="{ active: isActiveRoute('/profile') }"
                         :title="collapsed ? 'Mon Profil' : ''">
                        <div class="nav-item-content">
                            <div class="nav-icon">
                                <i class="pi pi-user-edit"></i>
                            </div>
                            <span v-if="!collapsed" class="nav-label">Mon Profil</span>
                        </div>
                    </div>
                    
                    <div class="nav-item logout-item" 
                         @click="logout" 
                         :title="collapsed ? 'Déconnexion' : ''">
                        <div class="nav-item-content">
                            <div class="nav-icon">
                                <i class="pi pi-sign-out"></i>
                            </div>
                            <span v-if="!collapsed" class="nav-label">Déconnexion</span>
                        </div>
                    </div>
                </div>
            </div>
        </div>
        
        <!-- Sidebar backdrop for mobile -->
        <div v-if="!collapsed" class="sidebar-backdrop" @click="toggleSidebar"></div>
    </aside>
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted } from "vue";
import { useRouter, useRoute } from "vue-router";
import AuthService from "../services/AuthService";
import { ThemeService } from "@/services/ThemeService";

const props = defineProps({
    collapsed: {
        type: Boolean,
        default: false,
    },
});

const emit = defineEmits(["toggle"]);

const router = useRouter();
const route = useRoute();
const isDarkMode = computed(() => ThemeService.getTheme() === "dark");

// Toggle sidebar collapse state
const toggleSidebar = () => {
    emit("toggle");
};

// Listen for theme changes
onMounted(() => {
    window.addEventListener("themechange", (e) => {
        isDarkMode.value = e.detail.theme === "dark";
    });
});

// Add authentication state tracker
const authState = ref(AuthService.isAuthenticated());
const updateAuthState = () => {
    authState.value = AuthService.isAuthenticated();
};

// Auth state computed property
const isAuthenticated = computed(() => authState.value);

// Update user reactivity
const authUser = ref(AuthService.getCurrentUser());
const updateUser = () => {
    authUser.value = AuthService.getCurrentUser();
};
const user = computed(() => authUser.value);

// Listen for auth state changes
onMounted(() => {
    window.addEventListener("auth-state-changed", updateAuthState);
    window.addEventListener("auth-state-changed", updateUser);
});

// Clean up event listeners when component is unmounted
onUnmounted(() => {
    window.removeEventListener("auth-state-changed", updateAuthState);
    window.removeEventListener("auth-state-changed", updateUser);
});

// Get user initials
const getUserInitials = (user) => {
    if (!user || !user.prenom || !user.nom) return 'U';
    return `${user.prenom.charAt(0)}${user.nom.charAt(0)}`.toUpperCase();
};

// Get role display name
const getRoleDisplayName = (role) => {
    const roleNames = {
        'AGENT_ANTENNE': 'Agent Antenne',
        'AGENT_GUC': 'Agent GUC',
'AGENT_COMMISSION_TERRAIN': 'Commission Vérification Terrain',        'SERVICE_TECHNIQUE': 'Service Technique',
        'ADMIN': 'Administrateur'
    };
    return roleNames[role] || role;
};

// Logout function
const logout = () => {
    AuthService.logout();
    window.dispatchEvent(new CustomEvent("auth-state-changed"));
    router.push("/");
};

// Navigation helper functions
const isActiveRoute = (routePath) => {
    if (!routePath) return false;
    
    if (routePath === route.path) return true;
    
    if (routePath === '/agent_antenne/dossiers/create') {
        return route.path === '/agent_antenne/dossiers/create' || route.path.startsWith('/agent_antenne/dossiers/create/');
    }
    
    if (routePath === '/agent_antenne/dossiers') {
        return route.path === '/agent_antenne/dossiers' || 
               (route.path.startsWith('/agent_antenne/dossiers/') && 
                !route.path.startsWith('/agent_antenne/dossiers/create'));
    }
    
    return route.path.startsWith(routePath + "/");
};

const navigateTo = (commandFn) => {
    if (typeof commandFn === "function") {
        commandFn();
    }
};

// Menu items based on user role
const menuItems = computed(() => {
    const role = user.value?.role;

    const items = [
        {
            label: "Tableau de bord",
            icon: "pi pi-th-large",
            command: () => router.push("/dashboard"),
            route: "/dashboard",
        },
    ];

    if (role === "AGENT_ANTENNE") {
        items.push(
            {
                label: "Mes Dossiers",
                icon: "pi pi-folder-open",
                command: () => router.push("/agent_antenne/dossiers"),
                route: "/agent_antenne/dossiers",
            },
            {
                label: "Créer un Dossier",
                icon: "pi pi-file-plus",
                command: () => router.push("/agent_antenne/dossiers/create"),
                route: "/agent_antenne/dossiers/create",
            }
        );
    } else if (role === "AGENT_GUC") {
        items.push(
             {
                label: "Mes Dossiers",
                icon: "pi pi-folder-open",
                command: () => router.push("/agent_guc/dossiers"),
                route: "/agent_guc/dossiers",
            }
        );
} else if (role === "AGENT_COMMISSION_TERRAIN") {        items.push(
            {
                label: "Mes Dossiers",
                icon: "pi pi-folder-open",
                command: () => router.push("/agent_commission/dossiers"),
                route: "/agent_commission/dossiers",
            },

            {
                label: "Visites de Terrain",
                icon: "pi pi-map-marker",
                command: () => router.push("/agent_commission/terrain-visits"),
                route: "/agent_commission/terrain-visits",
            }
       
        );
    } else if (role === "ADMIN") {
        items.push(
            {
                label: "Gestion Dossiers",
                icon: "pi pi-folder",
                command: () => router.push("/admin/documents-requis"),
                route: "/admin/documents-requis",
            }
        );
    }

    return items;
});
</script>

<style scoped>
.app-sidebar {
    width: 250px;
    background: var(--card-background);
    border-right: 1px solid var(--card-border);
    box-shadow: var(--shadow-sm);
    height: 100vh;
    position: fixed;
    left: 0;
    top: 0;
    z-index: 1000;
    overflow: hidden;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.app-sidebar.collapsed {
    width: 70px;
}

.sidebar-content {
    height: 100%;
    display: flex;
    flex-direction: column;
    position: relative;
    overflow: hidden;
}

/* Header section */
.sidebar-header {
    padding: 0.75rem;
    position: relative;
    border-bottom: 1px solid var(--clr-primary-a10);
}

.sidebar-logo {
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.collapsed .sidebar-logo {
    justify-content: center;
}

.logo-container {
    display: flex;
    align-items: center;
    gap: 12px;
}

.logo-icon {
    width: 40px;
    height: 40px;
    border-radius: var(--border-radius-md);
    display: flex;
    align-items: center;
    justify-content: center;
    backdrop-filter: blur(10px);
}

.logo-icon img {
    width: 40px;
    height: 40px;
    filter: var(--logo-filter);
}

.logo-icon-small {
    width: 32px;
    height: 32px;
    border-radius: var(--border-radius-md);
    display: flex;
    align-items: center;
    justify-content: center;
    backdrop-filter: blur(10px);
}

.logo-icon-small img {
    width: 20px;
    height: 20px;
    filter: var(--logo-filter);
}


.collapse-btn,
.expand-btn {
    border:none !important;
    color: var(--text-color);
    cursor: pointer;
    border-radius: var(--border-radius-md);
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.2s ease;
    background-color: transparent;
}

.collapse-btn:hover,
.expand-btn:hover {
    background: rgba(255, 255, 255, 0.2);
    border-color: rgba(255, 255, 255, 0.3);
}

.expand-btn-container {
    display: flex;
    justify-content: center;
    margin-top: 0.75rem;
}

/* User info section */
.user-info {
    padding: 0.9rem;
    border-bottom: 1px solid var(--card-border);
    display: flex;
    align-items: center;
    gap: 12px;
    background: var(--section-background);
}

.user-avatar {
    width: 35px;
    height: 35px;
    border-radius: 50%;
    background: var(--clr-primary-a0);
    display: flex;
    align-items: center;
    justify-content: center;
    color: var(--clr-light-a0);
    font-size: 0.875rem;
    font-weight: 600;
    box-shadow: var(--shadow-sm);
}

.user-initials {
    font-family: 'Poppins', sans-serif;
}

.user-details {
    display: flex;
    flex-direction: column;
    flex: 1;
}

.user-name {
    font-size: 0.875rem;
    font-weight: 600;
    color: var(--text-color);
    font-family: 'Poppins', sans-serif;
    line-height: 1.2;
}

.user-role {
    font-size: 0.75rem;
    color: var(--text-secondary);
    margin-top: 2px;
}

.user-status {
    display: flex;
    align-items: center;
}

.status-indicator {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: var(--success-color);
    box-shadow: 0 0 0 2px var(--card-background);
}

/* Navigation */
.sidebar-nav {
    flex: 1;
    padding: 0.5rem;
    overflow-y: auto;
    overflow-x: hidden;
}

.sidebar-nav::-webkit-scrollbar {
    width: 4px;
}

.sidebar-nav::-webkit-scrollbar-track {
    background: transparent;
}

.sidebar-nav::-webkit-scrollbar-thumb {
    background: var(--border-color);
    border-radius: var(--border-radius-sm);
}

.nav-section {
    display: flex;
    flex-direction: column;
    gap: 0.25rem;
}

.nav-item {
    position: relative;
    border-radius: var(--border-radius-md);
    cursor: pointer;
    transition: all 0.2s ease;
    overflow: hidden;
}

.nav-item:hover {
    background: var(--section-background);
}

.nav-item.active {
    background: var(--clr-primary-a0);
    color: var(--clr-light-a0);
    box-shadow: var(--shadow-sm);
}

.nav-item.active::before {
    content: '';
    position: absolute;
    left: 0;
    top: 0;
    width: 4px;
    height: 100%;
    background: var(--secondary-color);
    border-radius: 0 var(--border-radius-sm) var(--border-radius-sm) 0;
}

.nav-item-content {
    display: flex;
    align-items: center;
    padding: 0.75rem 1rem;
    position: relative;
    z-index: 2;
}

.collapsed .nav-item-content {
    justify-content: center;
    padding: 0.75rem 0.5rem;
}

.nav-icon {
    width: 20px;
    height: 20px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: var(--text-secondary);
    font-size: 1rem;
    transition: all 0.2s ease;
}

.nav-item:hover .nav-icon {
    color: var(--text-color);
}

.nav-item.active .nav-icon {
    color: var(--clr-light-a0);
}

.nav-label {
    margin-left: 0.875rem;
    font-size: 0.875rem;
    font-weight: 500;
    color: var(--text-color);
    transition: all 0.2s ease;
    flex: 1;
}

.nav-item:hover .nav-label {
    font-weight: 600;
}

.nav-item.active .nav-label {
    color: var(--clr-light-a0);
    font-weight: 600;
}

.nav-badge {
    background: var(--danger-color);
    color: var(--clr-light-a0);
    font-size: 0.6875rem;
    padding: 2px 6px;
    border-radius: 10px;
    font-weight: 600;
    box-shadow: var(--shadow-sm);
}

/* Footer */
.sidebar-footer {
    margin-top: auto;
    padding:  0.5rem;
    border-top: 1px solid var(--card-border);
    background: var(--section-background);
}

.footer-actions {
    display: flex;
    flex-direction: column;
    gap: 0.25rem;
}

.profile-item:hover {
    background: var(--clr-surface-tonal-a10);
}

.logout-item {
    color: var(--text-color);
}

.logout-item:hover {
    background: rgba(239, 68, 68, 0.1);
    border-left: 3px solid var(--danger-color);
}

.logout-item:hover .nav-icon,
.logout-item:hover .nav-label {
    color: var(--danger-color);
}

/* Sidebar backdrop for mobile */
.sidebar-backdrop {
    display: none;
}

/* Responsive design */
@media (max-width: 768px) {
    .app-sidebar {
        width: 100%;
        transform: translateX(-100%);
    }
    
    .app-sidebar:not(.collapsed) {
        transform: translateX(0);
    }
    
    .sidebar-backdrop {
        display: block;
        position: fixed;
        top: 0;
        left: 0;
        width: 100vw;
        height: 100vh;
        background: rgba(0, 0, 0, 0.5);
        z-index: -1;
    }
}

@media (max-width: 480px) {
    .app-sidebar {
        width: 85%;
    }
}

/* Accessibility improvements */
.nav-item:focus-visible {
    outline: 2px solid var(--primary-color);
    outline-offset: 2px;
}

.collapse-btn:focus-visible,
.expand-btn:focus-visible {
    outline: 2px solid var(--clr-light-a0);
    outline-offset: 2px;
}
</style>