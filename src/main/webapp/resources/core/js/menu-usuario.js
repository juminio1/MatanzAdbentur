// 1. Manejo de retroceso en el historial
window.addEventListener("pageshow", function (event) {
  const navegacionAtras =
    event.persisted ||
    (window.performance &&
      window.performance.getEntriesByType("navigation").length > 0 &&
      window.performance.getEntriesByType("navigation")[0].type === "back_forward");

  if (navegacionAtras) {
    window.location.replace(window.location.href);
  }
});

// 2. Delegación global para abrir y cerrar el menú
document.addEventListener("click", function (e) {
    const btnMenu = e.target.closest("#btnUserMenu");
    const menu = document.getElementById("userDropdown");

    if (!menu) return;

    if (btnMenu) {
        e.preventDefault();
        e.stopPropagation();

        const estaOculto = menu.classList.contains("oculto");
        if (estaOculto) {
            menu.classList.remove("oculto");
            btnMenu.setAttribute("aria-expanded", "true");
        } else {
            menu.classList.add("oculto");
            btnMenu.setAttribute("aria-expanded", "false");
        }
        return;
    }

    if (menu.contains(e.target)) {
        return;
    }

    if (!menu.classList.contains("oculto")) {
        menu.classList.add("oculto");
        const btn = document.getElementById("btnUserMenu");
        if (btn) btn.setAttribute("aria-expanded", "false");
    }
});

// 3. Delegación global para seleccionar y guardar el avatar
document.addEventListener("click", function (e) {
  const btn = e.target.closest(".btn-avatar-choice");
  if (!btn) return;

  e.preventDefault();
  e.stopPropagation();

  const nuevoAvatar = btn.getAttribute("data-avatar");
  if (!nuevoAvatar) return;

  // Cambia las imágenes en pantalla al instante
  document.querySelectorAll("#userNavAvatar, #dropdownUserAvatar, .user-avatar-btn img").forEach(img => {
    img.src = nuevoAvatar;
  });

  // Guarda en la base de datos
  const basePath = window.location.pathname.startsWith("/spring") ? "/spring" : "";
  fetch(basePath + "/api/usuario/avatar", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ avatar: nuevoAvatar })
  }).catch(err => console.error("Error al persistir avatar:", err));
});

// 4. Cargar datos de perfil al iniciar la vista (¡Con caché desactivada!)
async function cargarDatosPerfil() {
  try {
    const basePath = window.location.pathname.startsWith("/spring") ? "/spring" : "";
    const res = await fetch(basePath + "/api/usuario/perfil", { cache: "no-store" });

    if (res.ok) {
      const perfil = await res.json();
      const avatarUrl = perfil.avatar || "https://pub-d107f234b4134823bfda878a81c2c3de.r2.dev/default.png";

      document.querySelectorAll("#userNavAvatar, #dropdownUserAvatar, .user-avatar-btn img").forEach(img => {
        img.src = avatarUrl;
      });

      const txtNombre = document.getElementById("menuUserName");
      const txtRol = document.getElementById("menuUserRole");
      if (txtNombre && perfil.username) txtNombre.textContent = perfil.username;
      if (txtRol && perfil.rol) txtRol.textContent = perfil.rol;
    }
  } catch (err) {
    console.warn("No se pudo cargar datos del perfil inicial:", err);
  }
}

// Ejecutar al cargar
if (document.readyState === "loading") {
  document.addEventListener("DOMContentLoaded", cargarDatosPerfil);
} else {
  cargarDatosPerfil();
}