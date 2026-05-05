function fmtBytes(n) {
    if (n < 1024) return n + ' B';
    if (n < 1024 * 1024) return (n / 1024).toFixed(1) + ' KB';
    return (n / 1024 / 1024).toFixed(1) + ' MB';
}
function fmtTime(ms) { return new Date(ms).toLocaleTimeString(); }

function renderDir(id, files) {
    var tbody = document.getElementById(id);
    if (files.length === 0) {
        tbody.innerHTML = '<tr><td colspan="3" class="empty">empty</td></tr>';
        return;
    }
    tbody.innerHTML = files.map(function (f) {
        return '<tr data-dir="' + id + '" data-name="' + f.name + '">'
            + '<td><code>' + f.name + '</code></td>'
            + '<td>' + fmtBytes(f.size) + '</td>'
            + '<td>' + fmtTime(f.mtime) + '</td>'
            + '</tr>';
    }).join('');
    tbody.querySelectorAll('tr').forEach(function (tr) {
        tr.addEventListener('click', function () {
            showFile(tr.getAttribute('data-dir'), tr.getAttribute('data-name'));
        });
    });
}

function renderSessions(sessions) {
    var tbody = document.getElementById('sessions');
    if (!sessions.length) {
        tbody.innerHTML = '<tr><td colspan="4" class="empty">No active sessions</td></tr>';
        return;
    }
    tbody.innerHTML = sessions.map(function (s) {
        return '<tr><td><code>' + s.id + '</code></td>'
            + '<td>' + s.username + '</td>'
            + '<td><code>' + s.clientAddress + '</code></td>'
            + '<td>' + fmtTime(s.connectedAtMillis) + '</td></tr>';
    }).join('');
}

function showFile(dir, name) {
    fetch('/api/file?dir=' + encodeURIComponent(dir) + '&name=' + encodeURIComponent(name))
        .then(function (r) { return r.text(); })
        .then(function (text) {
            var el = document.getElementById('file-view');
            el.classList.add('fix-soh');
            el.innerHTML = '<strong>' + dir + '/' + name + '</strong>\n\n'
                + text.split('\u0001').join('<span class="soh">|</span>');
        })
        .catch(function (e) {
            document.getElementById('file-view').textContent = 'Error: ' + e;
        });
}

function tick() {
    fetch('/api/status')
        .then(function (r) { return r.json(); })
        .then(function (s) {
            document.getElementById('sftp-port').textContent = s.sftpPort;
            document.getElementById('root').textContent = s.root;
            renderSessions(s.sessions);
            renderDir('inbound', s.directories.inbound);
            renderDir('outbound', s.directories.outbound);
            renderDir('tp-inbound', s.directories['tp-inbound']);
            renderDir('tp-outbound', s.directories['tp-outbound']);
        })
        .catch(function () {});
}

tick();
setInterval(tick, 2000);
