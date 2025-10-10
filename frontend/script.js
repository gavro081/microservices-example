let stompClient = null;
const usernameField = document.querySelector("#username");
const productField = document.querySelector("#product");
const quantityField = document.querySelector("#quantity");
const orderStatus = document.querySelector("#orderStatus");
const form = document.querySelector("#orderForm");
const submitBtn = document.querySelector("#submitBtn");
const productTable = document.querySelector("#product-table");
const productTableBody = document.querySelector("#product-table-body");
const usersTable = document.querySelector("#product-table");
const usersTableBody = document.querySelector("#users-table-body");
const ordersTable = document.querySelector("#orders-table");
const ordersTableBody = document.querySelector("#orders-table-body");

const submitForm = async (e) => {
	e.preventDefault();
	submitBtn.disabled = true;
	orderStatus.innerText = "Placing order";
	const username = usernameField.value;
	connectAndSubscribe(username);
	await sendOrderRequest();
	submitBtn.disabled = false;
};

const sendOrderRequest = async () => {
	const username = usernameField.value;
	const productName = productField.value;
	const quantity = quantityField.value;

	const payload = {
		username,
		productName,
		quantity,
	};

	try {
		const response = await fetch("http://localhost:8080/api/orders", {
			method: "POST",
			headers: { "Content-Type": "application/json" },
			body: JSON.stringify(payload),
		});
		if (!response.ok) {
			throw new Error("response status: ", response.status);
		}
		const res = await response.json();
		orderStatus.innerText = "Order placed";
	} catch (err) {
		orderStatus.innerText = "Order failed";
		console.error(err);

		if (stompClient && stompClient.connected) {
			stompClient.disconnect(() => {
				console.log("disconnected due to error");
			});
		}
	}
};

form.addEventListener("submit", submitForm);

const connectAndSubscribe = (username) => {
	// gateway doesn't proxy websocket connections by default
	const socket = new SockJS("http://localhost:8083/ws");
	stompClient = Stomp.over(socket);

	stompClient.connect({}, (frame) => {
		const subscriptionUrl = "/topic/order-status/" + username;

		stompClient.subscribe(subscriptionUrl, (message) => {
			const update = JSON.parse(message.body);
			orderStatus.innerText = update.status;

			if (update.status == "COMPLETED" || update.status == "FAILED") {
				stompClient.disconnect(() => {
					console.log("disconnected");
					loadView();
				});
			}
		});
	});
};

let loadViewTimeout = null;

const loadView = async () => {
	if (loadViewTimeout) {
		clearTimeout(loadViewTimeout);
		loadViewTimeout = null;
	}

	productTableBody.innerHTML = "";
	usersTableBody.innerHTML = "";
	ordersTableBody.innerHTML = "";
	try {
		const response = await fetch("http://localhost:8080/api/products");
		const data = await response.json();
		renderProductsTable(data);
	} catch (err) {
		console.error("error: ", err);
		loadViewTimeout = setTimeout(loadView, 3000);
		return;
	}
	try {
		const response = await fetch("http://localhost:8080/api/users");
		const data = await response.json();
		renderUsersTable(data);
	} catch (err) {
		console.error("error: ", err);
		loadViewTimeout = setTimeout(loadView, 3000);
		return;
	}
	try {
		const response = await fetch("http://localhost:8080/api/orders/last");
		const data = await response.json();
		renderOrderTable(data);
	} catch (err) {
		console.error("error: ", err);
	}
	document.querySelector("#tables-wrapper").style.display = "flex";
};

const renderProductsTable = (products) => {
	products.forEach((element) => {
		const tr = document.createElement("tr");
		tr.innerHTML = `
        <td>${element.name}</td>
        <td>${element.price}</td>
        <td>${element.quantity}</td>
        <td>${element.category}</td>
        `;
		productTableBody.appendChild(tr);
	});
};

const renderUsersTable = (users) => {
	users.forEach((element) => {
		const tr = document.createElement("tr");
		tr.innerHTML = `
        <td>${element.username}</td>
        <td>${element.balance}</td>
        `;
		usersTableBody.appendChild(tr);
	});
};

const renderOrderTable = (order) => {
	const tr = document.createElement("tr");
	tr.innerHTML = `
        <td>${order.userId}</td>
        <td>${order.productId}</td>
        <td>${order.quantity}</td>
        <td>${order.status}</td>
        <td>${order.timestamp}</td>
        `;
	ordersTableBody.appendChild(tr);
};

loadView();
