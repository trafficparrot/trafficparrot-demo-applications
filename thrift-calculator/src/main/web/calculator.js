'use strict';

const totalLabel = document.getElementById('total');
const currentLabel = document.getElementById('current');
const messageLabel = document.getElementById('message');
const hostAndPortField = document.getElementById('host-and-port');
const digits = document.querySelectorAll('.digit');

// Requests go one at a time, in the order they were made, so the client sees the presses in order
let lastRequest = Promise.resolve();

function post(path, fields) {
    const request = lastRequest.then(() => fetch(path, {method: 'POST', body: new URLSearchParams(fields)}));
    lastRequest = request.catch(() => undefined);
    return request;
}

function paint(state) {
    totalLabel.textContent = 'Total: ' + state.total;
    currentLabel.textContent = 'Current: ' + state.current;
    messageLabel.textContent = state.message === null ? '' : state.message;
    if (state.host === '') {
        hostAndPortField.placeholder = 'Enter host:port of Thrift calculator server';
    } else {
        hostAndPortField.value = state.host + ':' + state.port;
    }
    digits.forEach(digit => {
        digit.disabled = state.digitsDisabled;
    });
}

function lostClient() {
    messageLabel.textContent = 'Cannot reach the calculator client. Is it still running?';
}

async function press(key) {
    try {
        const response = await post('api/press', {key: key});
        if (response.ok) {
            paint(await response.json());
        }
    } catch (e) {
        lostClient();
    }
}

document.querySelectorAll('button[data-key]').forEach(button => {
    button.addEventListener('click', () => press(button.dataset.key));
});

hostAndPortField.addEventListener('input', () => {
    post('api/server', {hostAndPort: hostAndPortField.value}).catch(lostClient);
});

fetch('api/state')
    .then(response => response.json())
    .then(paint)
    .catch(lostClient);
