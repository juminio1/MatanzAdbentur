// Si el usuario regresa con la flecha atrás del navegador, forzar recarga de estado
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

document.addEventListener("DOMContentLoaded", function () {
  const btnMenu = document.getElementById("btnUserMenu");
  const menu = document.getElementById("userDropdown");

  if (btnMenu && menu) {
    // 1. Alternar apertura/cierre al hacer clic en el botón del avatar
    btnMenu.addEventListener("click", function (e) {
      e.preventDefault();
      e.stopPropagation(); // Impide que el clic suba al document y se cierre inmediatamente

      const estaOculto = menu.classList.contains("oculto");
      if (estaOculto) {
        menu.classList.remove("oculto");
        btnMenu.setAttribute("aria-expanded", "true");
      } else {
        menu.classList.add("oculto");
        btnMenu.setAttribute("aria-expanded", "false");
      }
    });

    // 2. Prevenir que los clics dentro del menú lo cierren
    menu.addEventListener("click", function (e) {
      e.stopPropagation();
    });

    // 3. Cerrar el menú únicamente si se hace clic en cualquier elemento externo
    document.addEventListener("click", function (e) {
      if (!menu.contains(e.target) && !btnMenu.contains(e.target)) {
        menu.classList.add("oculto");
        btnMenu.setAttribute("aria-expanded", "false");
      }
    });

    // Cargar perfil y listeners de avatares
    cargarDatosPerfil();
    configurarSelectorAvatares();
  }
});

async function cargarDatosPerfil() {
  try {
    const basePath = window.location.pathname.startsWith("/spring") ? "/spring" : "";
    const respuesta = await fetch(basePath + "/api/usuario/perfil");

    if (respuesta.ok) {
      const perfil = await respuesta.json();
      const avatarUrl = perfil.avatar || "https://pub-d107f234b4134823bfda878a81c2c3de.r2.dev/default.png";

      const imgNav = document.getElementById("userNavAvatar");
      const imgDropdown = document.getElementById("dropdownUserAvatar");
      const txtNombre = document.getElementById("menuUserName");
      const txtRol = document.getElementById("menuUserRole");

      if (imgNav) imgNav.src = avatarUrl;
      if (imgDropdown) imgDropdown.src = avatarUrl;
      if (txtNombre && perfil.username) txtNombre.textContent = perfil.username;
      if (txtRol && perfil.rol) txtRol.textContent = perfil.rol;
    }
  } catch (error) {
    console.error("Error al cargar datos del perfil:", error);
  }
}

function configurarSelectorAvatares() {
  const botonesAvatar = document.querySelectorAll(".btn-avatar-choice");
  const basePath = window.location.pathname.startsWith("/spring") ? "/spring" : "";

  botonesAvatar.forEach((boton) => {
    boton.addEventListener("click", async function (e) {
      e.preventDefault();
      e.stopPropagation();

      const nuevoAvatar = boton.getAttribute("data-avatar");
      if (!nuevoAvatar) return;

      try {
        const respuesta = await fetch(basePath + "/api/usuario/avatar", {
          method: "POST",
          headers: {
            "Content-Type": "application/json"
          },
          body: JSON.stringify({ avatar: nuevoAvatar })
        });

        if (respuesta.ok) {
          const imgNav = document.getElementById("userNavAvatar");
          const imgDropdown = document.getElementById("dropdownUserAvatar");

          if (imgNav) imgNav.src = nuevoAvatar;
          if (imgDropdown) imgDropdown.src = nuevoAvatar;
        } else {
          console.error("Error del servidor al actualizar avatar:", respuesta.status);
        }
      } catch (error) {
        console.error("Error en la petición de avatar:", error);
      }
    });
  });
}
(function () {
  function vincularMenu() {
    var btn = document.getElementById("btnUserMenu");
    var menu = document.getElementById("userDropdown");
    if (!btn || !menu) return;

    // 1. Alternar visibilidad con el botón del avatar
    btn.onclick = function (e) {
      e.preventDefault();
      e.stopPropagation();
      menu.classList.toggle("oculto");
    };

    // 2. Prevenir que clics internos cierren el menú
    menu.onclick = function (e) {
      e.stopPropagation();
    };

    // 3. Cerrar al hacer clic en cualquier lugar externo
    document.addEventListener("click", function (e) {
      if (!menu.contains(e.target) && !btn.contains(e.target)) {
        menu.classList.add("oculto");
      }
    });

    // 4. Configurar selección de avatares
    var botones = menu.querySelectorAll(".btn-avatar-choice");
    var basePath = window.location.pathname.startsWith("/spring") ? "/spring" : "";

    botones.forEach(function (b) {
      b.onclick = async function (e) {
        e.preventDefault();
        e.stopPropagation();
        var nuevo = b.getAttribute("data-avatar");
        if (!nuevo) return;

        try {
          var resp = await fetch(basePath + "/api/usuario/avatar", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ avatar: nuevo })
          });

          if (resp.ok) {
            var imgNav = document.getElementById("userNavAvatar");
            var imgDrop = document.getElementById("dropdownUserAvatar");
            if (imgNav) imgNav.src = nuevo;
            if (imgDrop) imgDrop.src = nuevo;
          }
        } catch (err) {
          console.error("Error al actualizar avatar:", err);
        }
      };
    });

    // 5. Cargar datos del perfil en sesión
    fetch(basePath + "/api/usuario/perfil")
      .then(function (r) {
        return r.ok ? r.json() : null;
      })
      .then(function (perfil) {
        if (!perfil) return;
        var avatarUrl =
          perfil.avatar ||
          "https://pub-d107f234b4134823bfda878a81c2c3de.r2.dev/default.png";
        var imgNav = document.getElementById("userNavAvatar");
        var imgDrop = document.getElementById("dropdownUserAvatar");
        if (imgNav) imgNav.src = avatarUrl;
        if (imgDrop) imgDrop.src = avatarUrl;
      })
      .catch(function (err) {
        console.error("Error al cargar perfil:", err);
      });
  }

  // Ejecución segura sin depender únicamente de DOMContentLoaded
  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", vincularMenu);
  } else {
    vincularMenu();
  }
})();