// Función para enviar la selección de ficha al servidor (Spring MVC)
function seleccionarFicha(colorFicha) {
    const errorContainer = document.getElementById("mensajeFichaError");

    // Ocultar mensaje de error previo
    if (errorContainer) {
        errorContainer.setAttribute("hidden", "true");
        errorContainer.innerText = "";
    }

    // Petición al endpoint del Controlador de Spring
    fetch("/spring/sala-de-espera/elegir-ficha", {
        method: "POST",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded",
        },
        body: new URLSearchParams({
            ficha: colorFicha,
        }),
    })
        .then((response) => {
            if (!response.ok) {
                return response.text().then((text) => {
                    throw new Error(text || "La ficha seleccionada no está disponible.");
                });
            }
            // En lugar de response.json(), leemos como texto o vacío ya que el backend responde OK
            return response.text();
        })
        .then((data) => {
            // Si el backend responde bien, actualizamos la ficha seleccionada en la vista
            actualizarSeleccionVisual(colorFicha);
        })
        .catch((error) => {
            // Mostrar cartel de error usando el atributo hidden
            if (errorContainer) {
                errorContainer.innerText = error.message;
                errorContainer.removeAttribute("hidden");
            }
        });
}

// Función auxiliar para actualizar las clases visuales de la ficha activa
function actualizarSeleccionVisual(colorFicha) {
    const botones = document.querySelectorAll(".salaEspera__ficha");

    botones.forEach((btn) => {
        const check = btn.querySelector(".salaEspera__check");
        btn.classList.remove("salaEspera__ficha--seleccionada");

        if (check) {
            check.style.display = "none";
        }

        if (btn.getAttribute("data-ficha") === colorFicha) {
            btn.classList.add("salaEspera__ficha--seleccionada");
            if (check) {
                check.style.display = "flex";
            }
        }
    });
}
