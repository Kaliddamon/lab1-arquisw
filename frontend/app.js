const API_URL = "http://localhost:8080/api";

// Variable para guardar la cuenta actualmente seleccionada en el modal
let cuentaSeleccionadaModal = "";

// ================================
// Funciones de navegación
// ================================

function mostrarVista(nombreVista) {
    // Ocultar todas las vistas
    const vistas = document.querySelectorAll(".vista");
    vistas.forEach(v => v.classList.add("d-none"));

    // Mostrar la vista pedida
    const vistaActual = document.getElementById("vista-" + nombreVista);
    if (vistaActual) {
        vistaActual.classList.remove("d-none");
    }

    // Marcar el nav link activo
    document.querySelectorAll(".nav-link").forEach(link => link.classList.remove("active-link"));

    // Ejecutar la carga de datos si es necesario
    if (nombreVista === "clientes") {
        cargarClientes();
    }
}

// ================================
// Funciones de alerta
// ================================

function mostrarAlerta(mensaje, tipo) {
    // tipo puede ser: success, danger, warning, info
    const alerta = document.getElementById("alertaGlobal");
    alerta.className = "alert alert-" + tipo;
    alerta.innerHTML = mensaje;
    alerta.classList.remove("d-none");

    // Ocultar la alerta después de 4 segundos
    setTimeout(() => {
        alerta.classList.add("d-none");
    }, 4000);
}

// ================================
// VISTA 1: Consultar Clientes
// ================================

async function cargarClientes() {
    const tbody = document.getElementById("tbodyClientes");
    tbody.innerHTML = `<tr><td colspan="6" class="text-center"><div class="spinner-border text-primary" role="status"></div> Cargando clientes...</td></tr>`;

    try {
        const response = await fetch(API_URL + "/customers");
        
        if (!response.ok) {
            throw new Error("Error al obtener clientes: " + response.status);
        }

        const clientes = await response.json();

        if (clientes.length === 0) {
            tbody.innerHTML = `<tr><td colspan="6" class="text-center text-muted">No hay clientes registrados aún.</td></tr>`;
            return;
        }

        tbody.innerHTML = ""; // limpiar

        clientes.forEach(cliente => {
            const fila = document.createElement("tr");
            
            // Clase de saldo
            const saldoClase = cliente.balance >= 100 ? "saldo-alto" : "saldo-bajo";

            fila.innerHTML = `
                <td>${cliente.id}</td>
                <td>${cliente.firstName}</td>
                <td>${cliente.lastName}</td>
                <td><code>${cliente.accountNumber}</code></td>
                <td class="${saldoClase}">$${cliente.balance.toFixed(2)}</td>
                <td>
                    <button class="btn btn-sm btn-outline-primary me-1" onclick="verDetalleCliente(${cliente.id})">
                        <i class="bi bi-eye"></i> Ver
                    </button>
                    <button class="btn btn-sm btn-outline-info me-1" onclick="irHistorialDesdeCliente('${cliente.accountNumber}')">
                        <i class="bi bi-clock-history"></i>
                    </button>
                </td>
            `;
            tbody.appendChild(fila);
        });

    } catch (error) {
        tbody.innerHTML = `<tr><td colspan="6" class="text-center text-danger"><i class="bi bi-exclamation-triangle"></i> ${error.message}</td></tr>`;
        console.error("Error cargando clientes:", error);
    }
}

async function verDetalleCliente(id) {
    try {
        const response = await fetch(API_URL + "/customers/" + id);
        
        if (!response.ok) {
            throw new Error("No se encontró el cliente con ID " + id);
        }

        const cliente = await response.json();

        // Llenar el modal con los datos
        const cuerpoModal = document.getElementById("detalleClienteBody");
        cuerpoModal.innerHTML = `
            <div class="row">
                <div class="col-6"><strong>ID:</strong></div>
                <div class="col-6">${cliente.id}</div>
            </div>
            <hr>
            <div class="row">
                <div class="col-6"><strong>Nombre Completo:</strong></div>
                <div class="col-6">${cliente.firstName} ${cliente.lastName}</div>
            </div>
            <hr>
            <div class="row">
                <div class="col-6"><strong>N° Cuenta:</strong></div>
                <div class="col-6"><code>${cliente.accountNumber}</code></div>
            </div>
            <hr>
            <div class="row">
                <div class="col-6"><strong>Saldo Actual:</strong></div>
                <div class="col-6 ${cliente.balance >= 100 ? 'saldo-alto' : 'saldo-bajo'}">$${cliente.balance.toFixed(2)}</div>
            </div>
        `;

        // Guardar la cuenta para el botón "Ver Historial"
        cuentaSeleccionadaModal = cliente.accountNumber;
        document.getElementById("btnVerHistorialCliente").onclick = () => {
            irHistorialDesdeCliente(cuentaSeleccionadaModal);
            // cerrar el modal
            bootstrap.Modal.getInstance(document.getElementById("modalDetalleCliente")).hide();
        };

        // Mostrar el modal
        const modal = new bootstrap.Modal(document.getElementById("modalDetalleCliente"));
        modal.show();

    } catch (error) {
        mostrarAlerta("<i class='bi bi-x-circle'></i> " + error.message, "danger");
    }
}

// ================================
// VISTA 2: Crear Cliente
// ================================

document.addEventListener("DOMContentLoaded", () => {
    // Formulario crear cliente
    const formCrear = document.getElementById("formCrearCliente");
    formCrear.addEventListener("submit", async (e) => {
        e.preventDefault();

        const nombre = document.getElementById("inputNombre").value.trim();
        const apellido = document.getElementById("inputApellido").value.trim();
        const cuenta = document.getElementById("inputCuenta").value.trim();
        const saldo = parseFloat(document.getElementById("inputSaldo").value);

        // Validacion basica
        if (!nombre || !apellido || !cuenta) {
            mostrarAlerta("Por favor complete todos los campos.", "warning");
            return;
        }

        if (isNaN(saldo) || saldo < 0) {
            mostrarAlerta("El saldo debe ser un número positivo.", "warning");
            return;
        }

        const nuevoCliente = {
            firstName: nombre,
            lastName: apellido,
            accountNumber: cuenta,
            balance: saldo
        };

        try {
            const response = await fetch(API_URL + "/customers", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify(nuevoCliente)
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error("Error al crear cliente: " + errorText);
            }

            const clienteCreado = await response.json();
            mostrarAlerta(`<i class='bi bi-check-circle'></i> Cliente <strong>${clienteCreado.firstName} ${clienteCreado.lastName}</strong> creado exitosamente!`, "success");
            
            // Limpiar formulario
            formCrear.reset();

            // Ir a la vista de clientes
            setTimeout(() => {
                mostrarVista("clientes");
            }, 1500);

        } catch (error) {
            mostrarAlerta("<i class='bi bi-x-circle'></i> " + error.message, "danger");
        }
    });

    // Formulario transferencia
    const formTransfer = document.getElementById("formTransferencia");
    formTransfer.addEventListener("submit", async (e) => {
        e.preventDefault();

        const origen = document.getElementById("cuentaOrigen").value.trim();
        const destino = document.getElementById("cuentaDestino").value.trim();
        const monto = parseFloat(document.getElementById("montoTransferencia").value);

        if (!origen || !destino) {
            mostrarAlerta("Los números de cuenta son obligatorios.", "warning");
            return;
        }

        if (origen === destino) {
            mostrarAlerta("La cuenta origen y destino no pueden ser la misma.", "warning");
            return;
        }

        if (isNaN(monto) || monto <= 0) {
            mostrarAlerta("El monto debe ser mayor a 0.", "warning");
            return;
        }

        const transaccion = {
            senderAccountNumber: origen,
            receiverAccountNumber: destino,
            amount: monto
        };

        try {
            const response = await fetch(API_URL + "/transactions", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify(transaccion)
            });

            if (!response.ok) {
                const errorTexto = await response.text();
                throw new Error(errorTexto);
            }

            const resultado = await response.json();

            // Mostrar resultado
            document.getElementById("resId").textContent = resultado.id;
            document.getElementById("resOrigen").textContent = resultado.senderAccountNumber;
            document.getElementById("resDestino").textContent = resultado.receiverAccountNumber;
            document.getElementById("resMonto").textContent = resultado.amount.toFixed(2);
            
            // Formatear fecha
            if (resultado.timestamp) {
                const fecha = new Date(resultado.timestamp);
                document.getElementById("resFecha").textContent = fecha.toLocaleString("es-CO");
            } else {
                document.getElementById("resFecha").textContent = new Date().toLocaleString("es-CO");
            }

            document.getElementById("resultadoTransferencia").classList.remove("d-none");
            mostrarAlerta("<i class='bi bi-check-circle'></i> Transferencia realizada exitosamente!", "success");
            
            // Limpiar formulario
            formTransfer.reset();

        } catch (error) {
            document.getElementById("resultadoTransferencia").classList.add("d-none");
            mostrarAlerta("<i class='bi bi-x-circle'></i> " + error.message, "danger");
        }
    });

    // Mostrar la vista de clientes por defecto al cargar
    mostrarVista("clientes");
});

// ================================
// VISTA 3: Historial de Transacciones
// ================================

async function buscarHistorial() {
    const cuenta = document.getElementById("buscarCuentaHistorial").value.trim();
    
    if (!cuenta) {
        mostrarAlerta("Por favor ingrese un número de cuenta.", "warning");
        return;
    }

    const tbody = document.getElementById("tbodyHistorial");
    tbody.innerHTML = `<tr><td colspan="6" class="text-center"><div class="spinner-border text-info" role="status"></div> Buscando...</td></tr>`;

    try {
        const response = await fetch(API_URL + "/transactions/" + cuenta);
        
        if (!response.ok) {
            throw new Error("Error al buscar transacciones: " + response.status);
        }

        const transacciones = await response.json();

        if (transacciones.length === 0) {
            tbody.innerHTML = `<tr><td colspan="6" class="text-center text-muted"><i class="bi bi-inbox"></i> No se encontraron transacciones para la cuenta <code>${cuenta}</code></td></tr>`;
            document.getElementById("resumenHistorial").classList.add("d-none");
            return;
        }

        tbody.innerHTML = "";

        let totalRecibido = 0;
        let totalEnviado = 0;

        transacciones.forEach(tx => {
            const fila = document.createElement("tr");
            
            // Determinar si es envío o recepción
            const esEnviado = tx.senderAccountNumber === cuenta;
            const tipoBadge = esEnviado 
                ? `<span class="badge-enviado">Enviado</span>` 
                : `<span class="badge-recibido">Recibido</span>`;

            // Sumar totales
            if (esEnviado) {
                totalEnviado += tx.amount;
            } else {
                totalRecibido += tx.amount;
            }

            // Formatear fecha
            let fechaStr = "-";
            if (tx.timestamp) {
                const fecha = new Date(tx.timestamp);
                fechaStr = fecha.toLocaleString("es-CO");
            }

            fila.innerHTML = `
                <td>${tx.id}</td>
                <td><code>${tx.senderAccountNumber}</code></td>
                <td><code>${tx.receiverAccountNumber}</code></td>
                <td>$${tx.amount.toFixed(2)}</td>
                <td>${fechaStr}</td>
                <td>${tipoBadge}</td>
            `;
            tbody.appendChild(fila);
        });

        // Mostrar resumen
        const resumen = document.getElementById("resumenHistorial");
        resumen.classList.remove("d-none");
        document.getElementById("totalTx").textContent = transacciones.length;
        document.getElementById("totalRecibido").textContent = "$" + totalRecibido.toFixed(2);
        document.getElementById("totalEnviado").textContent = "$" + totalEnviado.toFixed(2);

    } catch (error) {
        tbody.innerHTML = `<tr><td colspan="6" class="text-center text-danger"><i class="bi bi-exclamation-triangle"></i> ${error.message}</td></tr>`;
        console.error("Error:", error);
    }
}

function limpiarHistorial() {
    document.getElementById("buscarCuentaHistorial").value = "";
    document.getElementById("tbodyHistorial").innerHTML = `
        <tr>
            <td colspan="6" class="text-center text-muted">
                <i class="bi bi-search"></i> Ingrese un número de cuenta para buscar
            </td>
        </tr>
    `;
    document.getElementById("resumenHistorial").classList.add("d-none");
}

// Función para ir al historial desde la vista de clientes
function irHistorialDesdeCliente(numeroCuenta) {
    mostrarVista("historial");
    document.getElementById("buscarCuentaHistorial").value = numeroCuenta;
    buscarHistorial();
}
