  const sidebar = document.getElementById('sidebar');
  const toggleBtn = document.getElementById('toggle-btn');
  const content = document.querySelector('.content');

  // Sidebar toggler
  toggleBtn.addEventListener('click', () => {
    sidebar.classList.toggle('collapsed');
    toggleBtn.innerHTML = sidebar.classList.contains('collapsed')
      ? '<i class="bi bi-chevron-right"></i>'
      : '<i class="bi bi-chevron-left"></i>';

        // Adjust content margin based on sidebar state
        if (sidebar.classList.contains('collapsed')) {
          content.style.marginLeft = '70px'; // Remove left margin
        } else {
          content.style.marginLeft = '250px'; // Example value, adjust as needed
        }
  });

  // Auto-collapse on scroll
  let lastScrollTop = 0;
  window.addEventListener('scroll', () => {
    const currentScroll = window.pageYOffset || document.documentElement.scrollTop;
    if (currentScroll > lastScrollTop) {
      sidebar.classList.add('collapsed');
      content.style.marginLeft = '70px'; // Remove left margin
    }
    lastScrollTop = currentScroll <= 0 ? 0 : currentScroll;
  });

