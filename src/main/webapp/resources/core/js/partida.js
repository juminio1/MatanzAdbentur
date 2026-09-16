document.addEventListener('DOMContentLoaded', () => {
  const btnAbrir = document.getElementById('btn-abrir-opciones');
  const btnCerrar = document.getElementById('btn-cerrar-opciones');
  const barraExtra = document.getElementById('barra-opciones-extra');

  btnAbrir.addEventListener('click', () => {
    btnAbrir.classList.add('oculto');
    barraExtra.classList.add('activa');
  });

  btnCerrar.addEventListener('click', () => {
    barraExtra.classList.remove('activa');
    btnAbrir.classList.remove('oculto');
  });
});