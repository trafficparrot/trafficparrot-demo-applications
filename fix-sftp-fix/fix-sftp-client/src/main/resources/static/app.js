function visualizeFix(text) {
    if (!text) return '';
    return text.split('\u0001').join('<span class="soh">|</span>');
}

function applyDefaults() {
    return fetch('/api/defaults').then(function (r) { return r.json(); }).then(function (d) {
        var f = document.getElementById('send-form');
        f.host.value = d.host;
        f.port.value = d.port;
        f.username.value = d.username;
        f.password.value = d.password;
        f.requestDir.value = d.requestDir;
        f.responseDir.value = d.responseDir;
        if (!f.clOrdId.value) f.clOrdId.value = 'DEMO-' + Date.now();
        if (!f.symbol.value) f.symbol.value = 'ACME';
        if (!f.qty.value) f.qty.value = '100';
        if (!f.price.value) f.price.value = '12.50';
    });
}

function renderLatest(result) {
    var el = document.getElementById('latest');
    var html = '';
    html += '<div class="meta">ClOrdID <code>' + result.clOrdId + '</code> &middot; Symbol <code>' + (result.symbol || '?') + '</code> &middot; '
            + 'Round trip: ' + (result.roundTripMillis !== undefined ? result.roundTripMillis : '?') + ' ms</div>';
    html += '<div class="label">Outgoing NewOrderSingle</div><pre>' + visualizeFix(result.outgoing) + '</pre>';
    if (result.error) {
        html += '<div class="label">Error</div><pre class="error">' + result.error + '</pre>';
    } else {
        html += '<div class="label">Incoming ExecutionReport</div><pre>' + visualizeFix(result.incoming) + '</pre>';
    }
    el.innerHTML = html;
}

function renderRecent(list) {
    var tbody = document.getElementById('recent');
    if (!list.length) { tbody.innerHTML = '<tr><td colspan="6" style="color:#9aa5b1">No history yet</td></tr>'; return; }
    tbody.innerHTML = list.map(function (r) {
        var when = new Date(r.startedAtMillis).toLocaleTimeString();
        var status = r.error ? '<span style="color:#b8242a">' + r.error + '</span>' : 'ok';
        return '<tr><td>' + when + '</td>'
            + '<td><code>' + r.clOrdId + '</code></td>'
            + '<td>' + (r.symbol || '?') + '</td>'
            + '<td><code>' + r.requestDir + '</code></td>'
            + '<td>' + status + '</td>'
            + '<td>' + (r.roundTripMillis || '?') + '</td></tr>';
    }).join('');
}

function pollRecent() {
    fetch('/api/recent').then(function (r) { return r.json(); }).then(renderRecent).catch(function () {});
}

function send(form) {
    var data = {};
    Array.prototype.forEach.call(form.elements, function (el) {
        if (el.name) data[el.name] = el.value;
    });
    var btn = document.getElementById('send-btn');
    btn.disabled = true;
    btn.textContent = 'Sending…';
    fetch('/api/send', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(data)
    })
        .then(function (r) { return r.json(); })
        .then(function (result) {
            renderLatest(result);
            pollRecent();
        })
        .catch(function (e) {
            renderLatest({error: String(e), outgoing: '', clOrdId: data.clOrdId});
        })
        .finally(function () {
            btn.disabled = false;
            btn.textContent = 'Send NewOrderSingle';
        });
}

document.getElementById('send-form').addEventListener('submit', function (e) {
    e.preventDefault();
    send(e.target);
    var f = document.getElementById('send-form');
    f.clOrdId.value = 'DEMO-' + Date.now();
});

applyDefaults().then(pollRecent);
setInterval(pollRecent, 2000);
