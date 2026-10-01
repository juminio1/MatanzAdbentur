// Si el usuario regresa con la flecha atras del navegador, forzar recarga
window.addEventListener("pageshow", function (event) {
  var navegacionAtras =
    event.persisted ||
    (window.performance &&
      window.performance.getEntriesByType("navigation").length > 0 &&
      window.performance.getEntriesByType("navigation")[0].type ===
        "back_forward");

  if (navegacionAtras) {
    window.location.replace(window.location.href);
  }
});

// Desplegable del menu de usuario
document.addEventListener("DOMContentLoaded", function () {
  const btnMenu = document.getElementById("btnUserMenu");
  const menu = document.getElementById("userDropdown");

  if (btnMenu && menu) {
    btnMenu.addEventListener("click", function (e) {
      e.stopPropagation();
      menu.classList.toggle("oculto");
    });

    document.addEventListener("click", function (e) {
      if (!menu.contains(e.target) && !btnMenu.contains(e.target)) {
        menu.classList.add("oculto");
      }
    });
  }
});