document.addEventListener("DOMContentLoaded", function () {

    
    const modalJugar = document.getElementById("modalJugar"); 
    const btnJugarAhora = document.getElementById("btnJugarAhora");
    const btnCerrarModal = document.getElementById("cerrarModal");

    
    if (btnJugarAhora && modalJugar) {
        btnJugarAhora.addEventListener("click", () => {
            modalJugar.classList.add("activo");
        });
    }

    
    if (btnCerrarModal && modalJugar) {
        btnCerrarModal.addEventListener("click", () => {
            modalJugar.classList.remove("activo");
        });
    }

    
    if (modalJugar) {
        modalJugar.addEventListener("click", (event) => {
            if (event.target === modalJugar) {
                modalJugar.classList.remove("activo");
            }
        });
    }

   
    const ventanaMenu = document.getElementById("ventanaMenu");
    const ventanaCodigo = document.getElementById("ventanaCodigo");
    const btnIrAUnirse = document.getElementById("btnIrAUnirse");
    const btnVolver = document.getElementById("btnVolver");

    
    if (btnIrAUnirse && ventanaMenu && ventanaCodigo) {
        btnIrAUnirse.addEventListener("click", () => {
            ventanaMenu.style.display = "none";
            ventanaCodigo.style.display = "block";
        });
    }

    
    if (btnVolver && ventanaMenu && ventanaCodigo) {
        btnVolver.addEventListener("click", () => {
            ventanaCodigo.style.display = "none";
            ventanaMenu.style.display = "block";
        });
    }

    
    const mensajeError = document.getElementById("mensajeErrorUnirse");
    
    if (mensajeError) {
        const modalJugar = document.getElementById("modalJugar");
        const ventanaMenu = document.getElementById("ventanaMenu");
        const ventanaCodigo = document.getElementById("ventanaCodigo");
        
      
        if (ventanaMenu && ventanaCodigo) {
            ventanaMenu.style.display = "none";
            ventanaCodigo.style.display = "block";
        }
        
       
        if (modalJugar) {
            try {
                
                const bsModal = new bootstrap.Modal(modalJugar);
                bsModal.show();
            } catch (e) {
                
                modalJugar.style.display = "block";
                modalJugar.classList.add("show");
                modalJugar.classList.add("activo");
            }
        }
    }
});