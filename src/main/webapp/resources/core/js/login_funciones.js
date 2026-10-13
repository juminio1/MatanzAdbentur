// Validación que ya tenías
export function validarCamposDeLogin(inputEmailValue, inputPasswordValue) {
  return inputEmailValue?.length > 0 && inputPasswordValue?.length > 0;
}

// Lógica para alternar visibilidad de contraseña
(function () {
  function inicializarTogglePassword() {
    const wrappers = document.querySelectorAll(".password-wrapper");

    const svgOjoAbierto = `
      <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#6c757d" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
          <circle cx="12" cy="12" r="3"></circle>
      </svg>
    `;

    const svgOjoTachado = `
      <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#2b3990" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path>
          <line x1="1" y1="1" x2="23" y2="23"></line>
      </svg>
    `;

    wrappers.forEach((wrapper) => {
      const input = wrapper.querySelector("input");
      const btn = wrapper.querySelector(".btn-toggle-pass");

      if (input && btn) {
        // Evita enlazar el listener más de una vez
        if (btn.dataset.initialized === "true") return;
        btn.dataset.initialized = "true";

        btn.addEventListener("click", (e) => {
          e.preventDefault();
          e.stopPropagation();

          const esPassword = input.getAttribute("type") === "password";
          input.setAttribute("type", esPassword ? "text" : "password");
          btn.innerHTML = esPassword ? svgOjoTachado : svgOjoAbierto;
        });
      }
    });
  }

  // Ejecución segura tanto si el DOM ya cargó como si está cargando
  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", inicializarTogglePassword);
  } else {
    inicializarTogglePassword();
  }
})();