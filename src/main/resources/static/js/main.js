document.addEventListener("DOMContentLoaded", function() {
    // Sidebar toggle for mobile
    const toggleBtn = document.getElementById("sidebarToggle");
    const sidebar = document.getElementById("sidebar");
    const overlay = document.getElementById("sidebarOverlay");

    if (toggleBtn && sidebar) {
        toggleBtn.addEventListener("click", function() {
            sidebar.classList.toggle("show");
            if(overlay) overlay.classList.toggle("show");
        });
    }

    if (overlay) {
        overlay.addEventListener("click", function() {
            sidebar.classList.remove("show");
            overlay.classList.remove("show");
        });
    }

    // Set active link in sidebar based on current URL path
    const currentPath = window.location.pathname;
    const navLinks = document.querySelectorAll(".sidebar-nav-link");
    navLinks.forEach(link => {
        const href = link.getAttribute("href");
        if (href) {
            // Check if current path matches href or ends with matching subdirectory
            if (currentPath === href || (href !== "/dashboard" && currentPath.startsWith(href))) {
                navLinks.forEach(l => l.classList.remove("active"));
                link.classList.add("active");
            }
        }
    });
});
