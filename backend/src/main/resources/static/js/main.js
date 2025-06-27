/**
 * OAuth2 SSO 前端脚本
 * =====================================
 */

// 全局变量
const app = {
    name: 'OAuth2 SSO',
    version: '1.0.0',
    baseUrl: window.location.origin,
    debug: false
};

// 工具函数
const utils = {
    /**
     * 显示消息提示
     * @param {string} message 消息内容
     * @param {string} type 消息类型 (success, error, warning, info)
     * @param {number} duration 显示时长(毫秒)
     */
    showMessage(message, type = 'info', duration = 5000) {
        const alertClass = type === 'error' ? 'danger' : type;
        const iconClass = {
            success: 'bi-check-circle',
            error: 'bi-exclamation-triangle',
            warning: 'bi-exclamation-triangle',
            info: 'bi-info-circle'
        }[type] || 'bi-info-circle';
        
        const alertHtml = `
            <div class="alert alert-${alertClass} alert-dismissible fade show" role="alert">
                <i class="bi ${iconClass}"></i>
                ${message}
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            </div>
        `;
        
        // 在页面顶部显示消息
        const container = document.querySelector('.container') || document.body;
        const alertDiv = document.createElement('div');
        alertDiv.innerHTML = alertHtml;
        container.insertBefore(alertDiv.firstElementChild, container.firstElementChild);
        
        // 自动消失
        if (duration > 0) {
            setTimeout(() => {
                const alert = container.querySelector('.alert');
                if (alert) {
                    const bsAlert = new bootstrap.Alert(alert);
                    bsAlert.close();
                }
            }, duration);
        }
    },

    /**
     * 格式化日期
     * @param {string|Date} date 日期
     * @param {string} format 格式
     */
    formatDate(date, format = 'YYYY-MM-DD HH:mm:ss') {
        const d = new Date(date);
        const year = d.getFullYear();
        const month = String(d.getMonth() + 1).padStart(2, '0');
        const day = String(d.getDate()).padStart(2, '0');
        const hours = String(d.getHours()).padStart(2, '0');
        const minutes = String(d.getMinutes()).padStart(2, '0');
        const seconds = String(d.getSeconds()).padStart(2, '0');
        
        return format
            .replace('YYYY', year)
            .replace('MM', month)
            .replace('DD', day)
            .replace('HH', hours)
            .replace('mm', minutes)
            .replace('ss', seconds);
    },

    /**
     * 防抖函数
     * @param {Function} func 要执行的函数
     * @param {number} wait 等待时间
     */
    debounce(func, wait) {
        let timeout;
        return function executedFunction(...args) {
            const later = () => {
                clearTimeout(timeout);
                func(...args);
            };
            clearTimeout(timeout);
            timeout = setTimeout(later, wait);
        };
    },

    /**
     * 节流函数
     * @param {Function} func 要执行的函数
     * @param {number} limit 时间间隔
     */
    throttle(func, limit) {
        let inThrottle;
        return function(...args) {
            if (!inThrottle) {
                func.apply(this, args);
                inThrottle = true;
                setTimeout(() => inThrottle = false, limit);
            }
        };
    },

    /**
     * 发送API请求
     * @param {string} url 请求URL
     * @param {object} options 请求选项
     */
    async apiRequest(url, options = {}) {
        const defaultOptions = {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            credentials: 'same-origin'
        };

        const config = { ...defaultOptions, ...options };
        
        if (config.body && typeof config.body === 'object') {
            config.body = JSON.stringify(config.body);
        }

        try {
            const response = await fetch(url, config);
            const data = await response.json();
            
            if (!response.ok) {
                throw new Error(data.message || `HTTP error! status: ${response.status}`);
            }
            
            return data;
        } catch (error) {
            console.error('API请求失败:', error);
            throw error;
        }
    },

    /**
     * 设置按钮加载状态
     * @param {HTMLElement} button 按钮元素
     * @param {boolean} loading 是否加载中
     */
    setButtonLoading(button, loading) {
        if (loading) {
            button.disabled = true;
            button.innerHTML = `
                <span class="spinner-border spinner-border-sm me-2" role="status"></span>
                加载中...
            `;
        } else {
            button.disabled = false;
            // 恢复原始文本（应该在调用前保存）
        }
    }
};

// API 服务
const api = {
    /**
     * 获取当前用户信息
     */
    async getCurrentUser() {
        return await utils.apiRequest('/api/user/current');
    },

    /**
     * 更新用户信息
     * @param {object} userData 用户数据
     */
    async updateUser(userData) {
        return await utils.apiRequest('/api/user/update', {
            method: 'POST',
            body: userData
        });
    },

    /**
     * 获取用户列表
     */
    async getUsers() {
        return await utils.apiRequest('/api/users');
    },

    /**
     * 搜索用户
     * @param {string} username 用户名关键字
     */
    async searchUsers(username) {
        return await utils.apiRequest(`/api/users/search?username=${encodeURIComponent(username)}`);
    },

    /**
     * 获取用户统计信息
     */
    async getUserStats() {
        return await utils.apiRequest('/api/users/stats');
    }
};

// UI 组件
const ui = {
    /**
     * 初始化工具提示
     */
    initTooltips() {
        const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
        tooltipTriggerList.map(function (tooltipTriggerEl) {
            return new bootstrap.Tooltip(tooltipTriggerEl);
        });
    },

    /**
     * 初始化弹出框
     */
    initPopovers() {
        const popoverTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="popover"]'));
        popoverTriggerList.map(function (popoverTriggerEl) {
            return new bootstrap.Popover(popoverTriggerEl);
        });
    },

    /**
     * 初始化滚动动画
     */
    initScrollAnimations() {
        const observerOptions = {
            threshold: 0.1,
            rootMargin: '0px 0px -50px 0px'
        };

        const observer = new IntersectionObserver((entries) => {
            entries.forEach(entry => {
                if (entry.isIntersecting) {
                    entry.target.classList.add('animate-fadeInUp');
                }
            });
        }, observerOptions);

        document.querySelectorAll('.card, .alert').forEach(el => {
            observer.observe(el);
        });
    },

    /**
     * 初始化搜索功能
     */
    initSearch() {
        const searchInput = document.querySelector('#searchInput');
        if (searchInput) {
            const debouncedSearch = utils.debounce(async (query) => {
                if (query.length > 2) {
                    try {
                        const results = await api.searchUsers(query);
                        // 处理搜索结果
                        console.log('搜索结果:', results);
                    } catch (error) {
                        utils.showMessage('搜索失败: ' + error.message, 'error');
                    }
                }
            }, 300);

            searchInput.addEventListener('input', (e) => {
                debouncedSearch(e.target.value);
            });
        }
    },

    /**
     * 初始化表单验证
     */
    initFormValidation() {
        const forms = document.querySelectorAll('.needs-validation');
        Array.from(forms).forEach(form => {
            form.addEventListener('submit', event => {
                if (!form.checkValidity()) {
                    event.preventDefault();
                    event.stopPropagation();
                }
                form.classList.add('was-validated');
            }, false);
        });
    }
};

// 页面特定功能
const pages = {
    /**
     * 首页功能
     */
    index() {
        // 统计数据动画
        const countElements = document.querySelectorAll('[data-count]');
        countElements.forEach(el => {
            const target = parseInt(el.dataset.count);
            const duration = 2000;
            const step = target / (duration / 16);
            let current = 0;
            
            const timer = setInterval(() => {
                current += step;
                if (current >= target) {
                    current = target;
                    clearInterval(timer);
                }
                el.textContent = Math.floor(current);
            }, 16);
        });
    },

    /**
     * 个人信息页面功能
     */
    profile() {
        // 用户信息编辑功能
        const editBtn = document.querySelector('#editProfileBtn');
        const saveBtn = document.querySelector('#saveProfileBtn');
        const cancelBtn = document.querySelector('#cancelProfileBtn');
        
        if (editBtn) {
            editBtn.addEventListener('click', () => {
                // 切换到编辑模式
                document.querySelectorAll('[data-editable]').forEach(el => {
                    const input = document.createElement('input');
                    input.type = 'text';
                    input.value = el.textContent;
                    input.className = 'form-control';
                    el.parentNode.replaceChild(input, el);
                });
                
                editBtn.style.display = 'none';
                saveBtn.style.display = 'inline-block';
                cancelBtn.style.display = 'inline-block';
            });
        }
        
        if (saveBtn) {
            saveBtn.addEventListener('click', async () => {
                const userData = {};
                document.querySelectorAll('input[type="text"]').forEach(input => {
                    // 收集表单数据
                });
                
                try {
                    utils.setButtonLoading(saveBtn, true);
                    await api.updateUser(userData);
                    utils.showMessage('用户信息更新成功', 'success');
                    location.reload();
                } catch (error) {
                    utils.showMessage('更新失败: ' + error.message, 'error');
                } finally {
                    utils.setButtonLoading(saveBtn, false);
                }
            });
        }
    },

    /**
     * 登录页面功能
     */
    login() {
        // 登录按钮点击效果
        const loginBtn = document.querySelector('.btn[href*="oauth2"]');
        if (loginBtn) {
            loginBtn.addEventListener('click', (e) => {
                utils.setButtonLoading(loginBtn, true);
                // OAuth2登录会跳转，所以不需要恢复按钮状态
            });
        }
    }
};

// 全局事件处理
const events = {
    /**
     * 处理全局错误
     */
    handleGlobalErrors() {
        window.addEventListener('error', (event) => {
            if (app.debug) {
                console.error('全局错误:', event.error);
            }
        });

        window.addEventListener('unhandledrejection', (event) => {
            if (app.debug) {
                console.error('未处理的Promise拒绝:', event.reason);
            }
        });
    },

    /**
     * 处理网络状态变化
     */
    handleNetworkStatus() {
        window.addEventListener('online', () => {
            utils.showMessage('网络连接已恢复', 'success', 3000);
        });

        window.addEventListener('offline', () => {
            utils.showMessage('网络连接已断开', 'warning', 5000);
        });
    },

    /**
     * 处理键盘快捷键
     */
    handleKeyboardShortcuts() {
        document.addEventListener('keydown', (e) => {
            // Ctrl/Cmd + / 显示快捷键帮助
            if ((e.ctrlKey || e.metaKey) && e.key === '/') {
                e.preventDefault();
                // 显示快捷键帮助
            }
            
            // ESC 关闭模态框
            if (e.key === 'Escape') {
                const modals = document.querySelectorAll('.modal.show');
                modals.forEach(modal => {
                    const bsModal = bootstrap.Modal.getInstance(modal);
                    if (bsModal) bsModal.hide();
                });
            }
        });
    }
};

// 主初始化函数
function init() {
    // 等待DOM加载完成
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
        return;
    }

    // 检测当前页面
    const path = window.location.pathname;
    const body = document.body;

    // 初始化UI组件
    ui.initTooltips();
    ui.initPopovers();
    ui.initScrollAnimations();
    ui.initSearch();
    ui.initFormValidation();

    // 初始化事件处理
    events.handleGlobalErrors();
    events.handleNetworkStatus();
    events.handleKeyboardShortcuts();

    // 根据页面执行特定功能
    if (path === '/' || path === '/index') {
        pages.index();
    } else if (path === '/profile') {
        pages.profile();
    } else if (path === '/login') {
        pages.login();
    }

    // 添加页面加载完成的类
    body.classList.add('page-loaded');

    // 调试信息
    if (app.debug) {
        console.log(`${app.name} v${app.version} 初始化完成`);
        console.log('当前页面:', path);
    }
}

// WebSocket 管理器
const websocket = {
    connection: null,
    reconnectAttempts: 0,
    maxReconnectAttempts: 5,
    heartbeatInterval: null,

    /**
     * 初始化WebSocket连接
     */
    init() {
        if (!this.isUserAuthenticated()) {
            return;
        }

        try {
            const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
            const wsUrl = `${protocol}//${window.location.host}/ws/logout`;
            
            this.connection = new WebSocket(wsUrl);
            
            this.connection.onopen = (event) => {
                console.log('WebSocket连接已建立');
                this.reconnectAttempts = 0;
                this.startHeartbeat();
                utils.showMessage('实时通知已启用', 'success', 2000);
            };
            
            this.connection.onmessage = (event) => {
                try {
                    const notification = JSON.parse(event.data);
                    this.handleNotification(notification);
                } catch (error) {
                    console.error('解析WebSocket消息失败:', error);
                }
            };
            
            this.connection.onclose = (event) => {
                console.log('WebSocket连接已关闭:', event.code, event.reason);
                this.stopHeartbeat();
                this.attemptReconnect();
            };
            
            this.connection.onerror = (error) => {
                console.error('WebSocket错误:', error);
            };
            
        } catch (error) {
            console.error('初始化WebSocket连接失败:', error);
        }
    },

    /**
     * 检查用户是否已认证
     */
    isUserAuthenticated() {
        return document.querySelector('.user-info') !== null;
    },

    /**
     * 开始心跳检测
     */
    startHeartbeat() {
        this.heartbeatInterval = setInterval(() => {
            if (this.connection && this.connection.readyState === WebSocket.OPEN) {
                this.connection.send('ping');
            }
        }, 30000);
    },

    /**
     * 停止心跳检测
     */
    stopHeartbeat() {
        if (this.heartbeatInterval) {
            clearInterval(this.heartbeatInterval);
            this.heartbeatInterval = null;
        }
    },

    /**
     * 尝试重连
     */
    attemptReconnect() {
        if (this.reconnectAttempts < this.maxReconnectAttempts) {
            setTimeout(() => {
                this.reconnectAttempts++;
                console.log(`尝试重连WebSocket (${this.reconnectAttempts}/${this.maxReconnectAttempts})`);
                this.init();
            }, 5000 * this.reconnectAttempts);
        } else {
            console.log('WebSocket重连次数已达上限');
            utils.showMessage('实时通知连接失败，请刷新页面', 'warning', 10000);
        }
    },

    /**
     * 处理通知消息
     */
    handleNotification(notification) {
        console.log('收到登出通知:', notification);
        
        switch (notification.type) {
            case 'CONNECTION_ESTABLISHED':
                console.log('WebSocket连接已确认');
                break;
                
            case 'FORCE_LOGOUT':
                this.handleForceLogout(notification);
                break;
                
            case 'BROADCAST_LOGOUT':
                this.handleBroadcastLogout(notification);
                break;
                
            case 'HEARTBEAT':
                // 心跳响应，无需处理
                break;
                
            default:
                console.log('未知通知类型:', notification.type);
        }
    },

    /**
     * 处理强制登出
     */
    handleForceLogout(notification) {
        const message = `您已在其他位置登出：${notification.message}`;
        
        // 创建强制登出模态框
        const modalHtml = `
            <div class="modal fade" id="forceLogoutModal" tabindex="-1" data-bs-backdrop="static" data-bs-keyboard="false">
                <div class="modal-dialog modal-dialog-centered">
                    <div class="modal-content">
                        <div class="modal-header bg-warning">
                            <h5 class="modal-title">
                                <i class="bi bi-exclamation-triangle"></i>
                                登出通知
                            </h5>
                        </div>
                        <div class="modal-body">
                            <p class="mb-0">${message}</p>
                            <small class="text-muted">页面将在3秒后自动跳转到登录页面</small>
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-primary" onclick="websocket.confirmForceLogout()">
                                立即跳转
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        `;
        
        document.body.insertAdjacentHTML('beforeend', modalHtml);
        const modal = new bootstrap.Modal(document.getElementById('forceLogoutModal'));
        modal.show();
        
        // 3秒后自动跳转
        setTimeout(() => {
            this.confirmForceLogout();
        }, 3000);
    },

    /**
     * 确认强制登出
     */
    confirmForceLogout() {
        this.cleanup();
        window.location.href = '/login?logout=forced';
    },

    /**
     * 处理广播登出
     */
    handleBroadcastLogout(notification) {
        const message = `系统通知：${notification.message}`;
        utils.showMessage(message, 'warning', 5000);
    },

    /**
     * 清理WebSocket连接
     */
    cleanup() {
        this.stopHeartbeat();
        if (this.connection) {
            this.connection.close();
            this.connection = null;
        }
    }
};

// 单点登出管理器
const ssoLogout = {
    /**
     * 初始化单点登出功能
     */
    init() {
        this.bindEvents();
        this.addSSOLogoutButton();
    },

    /**
     * 绑定事件
     */
    bindEvents() {
        // 绑定现有的登出按钮
        document.addEventListener('click', (e) => {
            if (e.target.matches('.sso-logout-btn') || e.target.closest('.sso-logout-btn')) {
                e.preventDefault();
                this.showLogoutOptions();
            }
        });
    },

    /**
     * 添加SSO登出按钮到现有的登出区域
     */
    addSSOLogoutButton() {
        const userInfo = document.querySelector('.user-info');
        if (userInfo) {
            const ssoButton = document.createElement('a');
            ssoButton.href = '/sso/logout';
            ssoButton.className = 'btn btn-outline-warning btn-sm ms-2 sso-logout-btn';
            ssoButton.innerHTML = '<i class="bi bi-shield-lock"></i> 单点登出';
            ssoButton.title = '安全登出所有会话';
            
            userInfo.appendChild(ssoButton);
        }
    },

    /**
     * 显示登出选项
     */
    showLogoutOptions() {
        const modalHtml = `
            <div class="modal fade" id="ssoLogoutModal" tabindex="-1">
                <div class="modal-dialog modal-lg">
                    <div class="modal-content">
                        <div class="modal-header">
                            <h5 class="modal-title">
                                <i class="bi bi-shield-lock"></i>
                                选择登出方式
                            </h5>
                            <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                        </div>
                        <div class="modal-body">
                            <div class="row g-3">
                                <div class="col-md-4">
                                    <div class="card h-100 logout-option" data-type="local">
                                        <div class="card-body text-center">
                                            <i class="bi bi-door-open display-4 text-info"></i>
                                            <h6 class="card-title mt-2">仅本地登出</h6>
                                            <p class="card-text small">只清除当前浏览器的登录状态</p>
                                        </div>
                                    </div>
                                </div>
                                <div class="col-md-4">
                                    <div class="card h-100 logout-option border-success" data-type="complete">
                                        <div class="card-body text-center">
                                            <i class="bi bi-door-closed display-4 text-success"></i>
                                            <h6 class="card-title mt-2">完整登出 <span class="badge bg-success">推荐</span></h6>
                                            <p class="card-text small">清除所有设备的登录会话</p>
                                        </div>
                                    </div>
                                </div>
                                <div class="col-md-4">
                                    <div class="card h-100 logout-option" data-type="global">
                                        <div class="card-body text-center">
                                            <i class="bi bi-shield-x display-4 text-danger"></i>
                                            <h6 class="card-title mt-2">全局登出</h6>
                                            <p class="card-text small">撤销令牌，最高安全级别</p>
                                        </div>
                                    </div>
                                </div>
                            </div>
                            <div class="alert alert-info mt-3">
                                <i class="bi bi-info-circle"></i>
                                <strong>推荐选择"完整登出"</strong>，它会清除所有设备上的登录会话，但保留OAuth2授权，下次登录更便捷。
                            </div>
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">取消</button>
                            <button type="button" class="btn btn-primary" id="confirmSSOLogout" disabled>
                                确认登出
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        `;
        
        // 移除现有模态框
        const existingModal = document.getElementById('ssoLogoutModal');
        if (existingModal) {
            existingModal.remove();
        }
        
        document.body.insertAdjacentHTML('beforeend', modalHtml);
        
        const modal = new bootstrap.Modal(document.getElementById('ssoLogoutModal'));
        modal.show();
        
        // 绑定选项点击事件
        let selectedType = 'complete'; // 默认选择完整登出
        document.querySelectorAll('.logout-option').forEach(option => {
            option.addEventListener('click', () => {
                document.querySelectorAll('.logout-option').forEach(opt => {
                    opt.classList.remove('border-primary', 'bg-light');
                });
                option.classList.add('border-primary', 'bg-light');
                selectedType = option.dataset.type;
                document.getElementById('confirmSSOLogout').disabled = false;
            });
        });
        
        // 默认选中推荐选项  
        document.querySelector('[data-type="complete"]').click();
        
        // 确认登出事件
        document.getElementById('confirmSSOLogout').addEventListener('click', () => {
            this.performLogout(selectedType);
            modal.hide();
        });
    },

    /**
     * 执行登出
     */
    async performLogout(type) {
        const confirmMessages = {
            local: '确认执行本地登出吗？',
            complete: '确认清除所有会话吗？这将登出所有设备上的会话。',
            global: '确认执行全局登出吗？这将撤销OAuth2令牌，需要重新完整授权。'
        };
        
        if (!confirm(confirmMessages[type])) {
            return;
        }
        
        try {
            // 显示加载状态
            utils.showMessage('正在执行登出操作...', 'info', 0);
            
            // 发送登出请求
            const response = await utils.apiRequest('/sso/api/logout', {
                method: 'POST',
                body: { logoutType: type }
            });
            
            if (response.success) {
                utils.showMessage('登出成功', 'success', 2000);
                
                // 清理本地状态
                websocket.cleanup();
                
                // 延迟跳转
                setTimeout(() => {
                    window.location.href = '/login?logout=success';
                }, 1000);
            } else {
                utils.showMessage('登出失败: ' + response.message, 'error');
            }
            
        } catch (error) {
            utils.showMessage('登出失败: ' + error.message, 'error');
        }
    }
};

// 更新主初始化函数
function init() {
    // 等待DOM加载完成
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
        return;
    }

    // 检测当前页面
    const path = window.location.pathname;
    const body = document.body;

    // 初始化UI组件
    ui.initTooltips();
    ui.initPopovers();
    ui.initScrollAnimations();
    ui.initSearch();
    ui.initFormValidation();

    // 初始化事件处理
    events.handleGlobalErrors();
    events.handleNetworkStatus();
    events.handleKeyboardShortcuts();

    // 初始化WebSocket（仅在已登录时）
    if (websocket.isUserAuthenticated()) {
        websocket.init();
        ssoLogout.init();
    }

    // 根据页面执行特定功能
    if (path === '/' || path === '/index') {
        pages.index();
    } else if (path === '/profile') {
        pages.profile();
    } else if (path === '/login') {
        pages.login();
    }

    // 添加页面加载完成的类
    body.classList.add('page-loaded');

    // 调试信息
    if (app.debug) {
        console.log(`${app.name} v${app.version} 初始化完成`);
        console.log('当前页面:', path);
    }
}

// 页面卸载时清理
window.addEventListener('beforeunload', () => {
    websocket.cleanup();
});

// 导出到全局
window.OAuth2SSO = {
    app,
    utils,
    api,
    ui,
    pages,
    events,
    websocket,
    ssoLogout,
    init
};

// 自动初始化
init(); 