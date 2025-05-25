<template>
    <aside v-if="isAuthenticated" class="app-sidebar" :class="{ collapsed: collapsed }">
        <div class="sidebar-content">
            <!-- Logo section with collapse toggle -->
            <div class="sidebar-logo">
                <!-- Full logo (visible when expanded) -->
                <template v-if="!collapsed">
                    <router-link to="/" class="logo-link">
                        <img v-if="isDarkMode" src="@/assets/logo/sadsa.svg" alt="sadsa" />
                        <img v-else src="@/assets/logo/sadsa.svg" alt="sadsa" />
                    </router-link>
                    <button class="collapse-btn" @click="toggleSidebar">
                        <i class="pi pi-angle-double-left"></i>
                    </button>
                </template>

                <!-- Small logo (visible when collapsed) -->
                <template v-else>
                    <router-link to="/" class="logo-link">
                        <img v-if="isDarkMode" src="@/assets/logo/sadsa.svg" alt="sadsa" />
                        <img v-else src="@/assets/logo/sadsa.svg" alt="sadsa" />
                    </router-link>
                </template>
            </div>

            <!-- Expand button (only visible when collapsed) -->
            <div v-if="collapsed" class="expand-btn-container">
                <button class="expand-btn" @click="toggleSidebar">
                    <i class="pi pi-angle-double-right"></i>
                </button>
            </div>

            <!-- Menu items -->
            <div class="sidebar-menu">
                <div v-for="(item, index) in menuItems" :key="index" class="menu-item"
                    :class="{ active: isActiveRoute(item.route) }" @click="navigateTo(item.command)"
                    :title="collapsed ? item.label : ''">
                    <i :class="item.icon"></i>
                    <span v-if="!collapsed">{{ item.label }}</span>
                </div>
            </div>

            <!-- Logout button -->
            <div class="sidebar-footer">
                <div class="menu-item" @click="router.push('/profile')" :class="{ active: isActiveRoute('/profile') }"
                    :title="collapsed ? 'Profil' : ''">
                    <i class="pi pi-user-edit"></i>
                    <span v-if="!collapsed">Mon Profil</span>
                </div>
                <div class="menu-item logout-item" @click="logout" :title="collapsed ? 'Déconnexion' : ''">
                    <i class="pi pi-sign-out"></i>
                    <span v-if="!collapsed">Déconnexion</span>
                </div>
            </div>
        </div>
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

// Logout function
const logout = () => {
    AuthService.logout();
    // Emit a custom event that components can listen to
    window.dispatchEvent(new CustomEvent("auth-state-changed"));
    router.push("/");
};

// Navigation helper functions
const isActiveRoute = (routePath) => {
    if (!routePath) return false;
    return route.path === routePath || route.path.startsWith(routePath + "/");
};

const navigateTo = (commandFn) => {
    if (typeof commandFn === "function") {
        commandFn();
    }
};

// Menu items based on user role
const menuItems = computed(() => {
    const role = user.value?.role;

    // Default menu items for all users
    const items = [
        {
            label: "Tableau de bord",
            icon: "pi pi-th-large",
            command: () => router.push("/dashboard"),
            route: "/dashboard",
        },
    ];

    // Role-specific menu items
    if (role === "AGENT_ANTENNE") {
        items.push(
            {
                label: "Créer un Dossier",
                icon: "pi pi-file",
                command: () => router.push("/agent_antenne/dossiers/create"),
                route: "/agent_antenne/dossiers/create",
            },
            {
                label: "Rapport Final",
                icon: "pi pi-file-pdf",
                command: () => router.push("/etudiant/rapport"),
                route: "/etudiant/rapport",
            }
        );
    } else if (role === "AGENT_GUC") {
        items.push(
            {
                label: "Révision des documents",
                icon: "pi pi-check-square",
                command: () => router.push("/encadrant/document-evaluation"),
                route: "/encadrant/document-evaluation",
            },
            {
                label: "Évaluation des Binômes",
                icon: "pi pi-check-circle",
                command: () => router.push("/encadrant/grading"),
                route: "/encadrant/grading",
            }
        );
    } else if (role === "AGENT_COMMISSION") {
        items.push(
            {
                label: "Gestion des Comptes",
                icon: "pi pi-user",
                command: () => router.push("/management/comptes"),
                route: "/management/comptes",
            },
            {
                label: "Gestion des Binômes",
                icon: "pi pi-users",
                command: () => router.push("/management/binomes"),
                route: "/management/binomes",
            },
            {
                label: "Gestion des Sujets",
                icon: "pi pi-file-o",
                command: () => router.push("/management/sujets"),
                route: "/management/sujets",
            },
            {
                label: "Suggestions de Sujets",
                icon: "pi pi-inbox",
                command: () => router.push("/management/sujet-suggestions"),
                route: "/management/sujet-suggestions",
            },
            {
                label: "Gestion des Soutenances",
                icon: "pi pi-calendar",
                command: () => router.push("/management/soutenances"),
                route: "/management/soutenances",
            },
            {
                label: "Gestion des Notes",
                icon: "pi pi-chart-line",
                command: () => router.push("/management/notes-management"),
                route: "/management/notes-management",
            },
            {
                label: "Archive",
                icon: "pi pi-building-columns",
                command: () => router.push("/management/archive"),
                route: "/management/archive",
            },
            {
                label: "Paramètres",
                icon: "pi pi-cog",
                command: () => router.push("/management/settings"),
                route: "/management/settings",
            }
        );
    } else if (role === "ADMIN") {
        items.push(
            {
                label: "Mes Soutenances",
                icon: "pi pi-calendar-plus",
                command: () => router.push("/jury/soutenances"),
                route: "/jury/soutenances",
            },
            {
                label: "Évaluation Soutenances",
                icon: "pi pi-microphone",
                command: () => router.push("/jury/grading"),
                route: "/jury/grading",
            },
            {
                label: "Évaluation Rapports",
                icon: "pi pi-file-pdf",
                command: () => router.push("/jury/report-evaluation"),
                route: "/jury/report-evaluation",
            }
        );
    }

    return items;
});
</script>

<style scoped>
.app-sidebar {
    width: 250px;
    background-color: #ffffff;
    border-right: 1px solid #f0f0f0;
    box-shadow: 0 0 15px rgba(0, 0, 0, 0.05);
    height: 100vh;
    position: fixed;
    left: 0;
    top: 0;
    z-index: 990;
    overflow-y: auto;
    overflow-x: hidden;
    padding: 0;
    transition: width 0.3s ease;
}

.app-sidebar.collapsed {
    width: 60px;
}

.dark-mode .app-sidebar {
    background-color: #1a1a1a;
    border-right: 1px solid #2c2c2c;
    box-shadow: 0 0 15px rgba(0, 0, 0, 0.15);
}

.sidebar-content {
    padding: 0;
    position: relative;
    display: flex;
    flex-direction: column;
    height: 100%;
}

/* Logo section styles */
.sidebar-logo {
    padding: 16px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    position: relative;
}

.collapsed .sidebar-logo {
    justify-content: center;
    padding: 16px 10px;
}

.logo-link {
    display: inline-block;
    transition: opacity 0.2s ease;
}

.logo-link:hover {
    opacity: 0.8;
}

.sidebar-logo img {
    height: auto;
    width: 100%;
    transition: all 0.3s ease;
    display: block;
}

.collapsed .sidebar-logo img {
    width: 30px;
    height: 30px;
}

.collapse-btn {
    background: transparent;
    border: none;
    color: #666;
    cursor: pointer;
    font-size: 18px;
    padding: 10px;
    margin-left: 5px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.2s ease;
}

.collapse-btn:hover {
    background-color: #f5f5f5;
    color: var(--primary-color);
}

.dark-mode .collapse-btn:hover {
    background-color: #2a2a2a;
}

/* Expand button (visible when collapsed) */
.expand-btn-container {
    display: flex;
    justify-content: center;
}

.expand-btn {
    background: transparent;
    border: none;
    color: #666;
    cursor: pointer;
    font-size: 18px;
    padding: 10px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.2s ease;
}

.expand-btn:hover {
    background-color: #f5f5f5;
    color: var(--primary-color);
}

.dark-mode .expand-btn:hover {
    background-color: #2a2a2a;
}

/* Menu styles */
.sidebar-menu {
    padding: 10px 0;
    flex: 1;
}

.menu-item {
    display: flex;
    align-items: center;
    padding: 12px 20px;
    cursor: pointer;
    color: #666;
    font-size: 14px;
    position: relative;
    transition: all 0.2s ease;
    white-space: nowrap;
}

.collapsed .menu-item {
    padding: 12px 0;
    justify-content: center;
}

.menu-item:hover {
    background-color: #f8f9fa;
    color: var(--primary-color);
}

.dark-mode .menu-item:hover {
    background-color: #2a2a2a;
}

.menu-item.active {
    color: var(--primary-color);
    background-color: rgba(67, 97, 238, 0.1);
    font-weight: 500;
}

.dark-mode .menu-item.active {
    background-color: rgba(67, 97, 238, 0.15);
}

.menu-item.active::before {
    content: "";
    position: absolute;
    left: 0;
    top: 0;
    width: 4px;
    height: 100%;
    background-color: var(--primary-color);
    border-radius: 0 4px 4px 0;
}

.menu-item i {
    font-size: 18px;
    width: 20px;
    text-align: center;
}

.collapsed .menu-item i {
    margin-right: 0;
}

.menu-item span {
    margin-left: 12px;
    transition: opacity 0.3s ease;
}

.collapsed .menu-item span {
    display: none;
}

/* Footer/Logout styles */
.sidebar-footer {
    margin-top: auto;
    padding: 10px 0;
    border-top: 1px solid #f0f0f0;
}

.dark-mode .sidebar-footer {
    border-top: 1px solid #2c2c2c;
}

.logout-item {
    color: #666;
}

.logout-item:hover {
    color: #f44336;
    background-color: rgba(244, 67, 54, 0.1);
}

.dark-mode .logout-item:hover {
    background-color: rgba(244, 67, 54, 0.15);
}

@media (max-width: 768px) {
    .app-sidebar {
        width: 100%;
        height: auto;
        position: relative;
        border-radius: 0;
        border-right: none;
        box-shadow: 0 2px 5px rgba(0, 0, 0, 0.05);
    }

    .app-sidebar.collapsed {
        width: 100%;
    }

    .sidebar-content {
        height: auto;
    }

    .collapsed .sidebar-logo,
    .sidebar-logo {
        padding: 16px;
        justify-content: space-between;
    }

    .collapsed .menu-item {
        padding: 10px 16px;
        justify-content: flex-start;
    }

    .collapsed .menu-item span {
        display: block;
        margin-left: 12px;
    }

    .expand-btn-container {
        display: none;
    }
}
</style>