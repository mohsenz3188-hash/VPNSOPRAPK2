(function () {
  'use strict';

  const routes = {
    dashboard: 'dashboard',
    configs: 'configs',
    subscriptions: 'subscriptions',
    settings: 'settings',
    logs: 'logs',
    rules: 'rules',
    rule_add: 'rule_add',
    downloads: 'apk_portal',
    architectures: 'apk_portal',
    'integrity-pgp': 'apk_portal',
    'engine-specs': 'apk_portal',
    changelog: 'apk_portal',
    'github-source': 'apk_portal'
  };

  const safe = (value) => String(value == null ? '' : value)
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;').replace(/'/g, '&#39;');

  function native() {
    return typeof window.Android !== 'undefined';
  }

  function toast(message) {
    const box = document.getElementById('toastNotification') || document.getElementById('toast-banner');
    const label = document.getElementById('toastMessage') || document.getElementById('toast-message');
    if (box && label) {
      label.textContent = message;
      box.classList.remove('hidden');
      box.classList.add('flex');
      window.setTimeout(() => { box.classList.add('hidden'); box.classList.remove('flex'); }, 2600);
    } else if (native()) {
      Android.showToast(message);
    }
  }

  window.vpnState = function (state, message) {
    const status = String(state || 'DISCONNECTED').toUpperCase();
    const connected = status === 'CONNECTED';
    const connecting = status === 'CONNECTING' || status === 'STARTING';
    const orbText = document.getElementById('orb-status-text');
    const powerIcon = document.getElementById('power-icon');
    const statusText = document.querySelector('.text-tertiary-fixed.uppercase.truncate');
    if (orbText) {
      orbText.textContent = connected ? 'SECURED' : (connecting ? 'CONNECTING' : (status === 'ERROR' ? 'ERROR' : 'CONNECT'));
      orbText.className = 'font-label-caps text-label-caps uppercase tracking-widest mt-1 ' + (connected ? 'text-primary-fixed' : (status === 'ERROR' ? 'text-error' : 'text-on-surface-variant'));
    }
    if (powerIcon) {
      powerIcon.className = 'material-symbols-outlined text-[42px] transition-transform duration-300 ' + (connected ? 'text-primary-container' : (status === 'ERROR' ? 'text-error' : 'text-on-surface-variant'));
    }
    if (statusText) statusText.textContent = 'STATUS: ' + (connected ? 'SHIELD ACTIVE' : (connecting ? 'CONNECTING' : (status === 'ERROR' ? 'CONNECTION ERROR' : 'DISCONNECTED')));
    const orb = document.getElementById('vpn-power-orb');
    if (orb) orb.setAttribute('aria-label', connected ? 'Disconnect VPN' : 'Connect VPN');
    if (message && status === 'ERROR') toast(message);
  };

  function renderProfiles() {
    if (!native()) return;
    const host = document.getElementById('nodesContainer');
    if (!host) return;
    let profiles = [];
    try { profiles = JSON.parse(Android.getProfilesJson() || '[]'); } catch (_) {}
    if (!profiles.length) {
      host.innerHTML = '<div class="rounded-xl border border-dashed border-outline-variant/50 bg-surface-container-low p-5 text-center"><div class="text-primary-container text-2xl mb-2">＋</div><div class="font-headline-md text-headline-md text-on-surface mb-1">No VPN profile yet</div><p class="font-mono-sm text-mono-sm text-on-surface-variant">Import a VLESS / VMess / Trojan / Shadowsocks link or sing-box JSON configuration.</p></div>';
      return;
    }
    host.innerHTML = profiles.map((p) => {
      const selected = !!p.selected;
      return '<div role="button" tabindex="0" class="node-card w-full text-left rounded-xl p-4 mb-3 border cursor-pointer ' +
        (selected ? 'bg-surface-container border-primary-container/60 shadow-[0_0_16px_rgba(0,245,212,0.14)]' : 'bg-surface-container-low border-outline-variant/30') +
        '" data-profile-id="' + safe(p.id) + '" data-name="' + safe(p.name) + '" data-protocol="' + safe(p.protocol) + '">' +
        '<div class="flex items-center justify-between gap-3"><div class="min-w-0"><div class="font-headline-md text-headline-md text-on-surface truncate">' + safe(p.name) + '</div>' +
        '<div class="font-mono-sm text-mono-sm text-on-surface-variant mt-1">' + safe(p.protocol) + '</div></div>' +
        '<div class="flex items-center gap-2"><span class="font-label-caps text-label-caps rounded px-2 py-1 ' + (selected ? 'bg-primary-container/15 text-primary-container' : 'bg-surface-container-high text-on-surface-variant') + '">' + (selected ? 'SELECTED' : 'SELECT') + '</span>' +
        '<button type="button" data-delete-profile="' + safe(p.id) + '" class="rounded bg-error-container/30 text-error px-2 py-1 font-label-caps text-label-caps">DELETE</button></div></div></div>';
    }).join('');
    host.querySelectorAll('[data-profile-id]').forEach((card) => {
      card.addEventListener('click', () => {
        Android.selectProfile(card.getAttribute('data-profile-id'));
        renderProfiles();
        toast('Active profile updated');
      });
    });
    host.querySelectorAll('[data-delete-profile]').forEach((button) => {
      button.addEventListener('click', (event) => {
        event.preventDefault(); event.stopPropagation();
        Android.deleteProfile(button.getAttribute('data-delete-profile'));
      });
    });
    const search = document.getElementById('nodeSearch');
    if (search && !search.dataset.vpnsoprBound) {
      search.dataset.vpnsoprBound = '1';
      search.addEventListener('input', () => {
        const term = search.value.toLowerCase().trim();
        host.querySelectorAll('[data-profile-id]').forEach((card) => {
          const haystack = (card.getAttribute('data-name') + ' ' + card.getAttribute('data-protocol')).toLowerCase();
          card.style.display = haystack.includes(term) ? '' : 'none';
        });
      });
    }
  }

  function attachSettingsShortcuts() {
    if (!document.getElementById('routingModeGroup')) return;
    const main = document.querySelector('main');
    if (!main || document.getElementById('vpnsopr-advanced-shortcuts')) return;
    const panel = document.createElement('section');
    panel.id = 'vpnsopr-advanced-shortcuts';
    panel.className = 'mx-4 mb-8 rounded-xl bg-surface-container-low p-4';
    panel.innerHTML = '<div class="font-headline-md text-headline-md text-primary-container mb-3">ADVANCED TOOLS</div>' +
      '<div class="grid grid-cols-2 gap-2"><button data-go="rules" class="rounded-lg bg-surface-container-high p-3 text-left text-on-surface">Rule Inspector</button>' +
      '<button data-go="logs" class="rounded-lg bg-surface-container-high p-3 text-left text-on-surface">Terminal Logs</button>' +
      '<button data-go="rule_add" class="rounded-lg bg-surface-container-high p-3 text-left text-on-surface">Add Override</button>' +
      '<button data-go="configs" class="rounded-lg bg-surface-container-high p-3 text-left text-on-surface">Manage Profiles</button></div>';
    main.appendChild(panel);
    panel.querySelectorAll('[data-go]').forEach((button) => button.addEventListener('click', () => {
      if (native()) Android.navigate(button.getAttribute('data-go'));
    }));
  }

  document.addEventListener('click', function (event) {
    const pathLink = event.target.closest && event.target.closest('[data-path]');
    if (pathLink) {
      const path = pathLink.getAttribute('data-path');
      if (routes[path] && native()) {
        event.preventDefault(); event.stopPropagation(); event.stopImmediatePropagation();
        Android.navigate(path);
        return;
      }
    }

    const id = event.target.closest && event.target.closest('#vpn-power-orb, #btnImportClipboard, #btnNewManual, #btnScanQr, #addFeedBtn, #syncNowBtn, #btn-restart-core');
    if (!id || !native()) return;
    event.preventDefault(); event.stopPropagation(); event.stopImmediatePropagation();
    if (id.id === 'vpn-power-orb') Android.toggleVpn();
    else if (id.id === 'btnImportClipboard') Android.importClipboard();
    else if (id.id === 'btnNewManual') Android.promptImport();
    else if (id.id === 'btnScanQr') Android.showToast('اسکن دوربین QR هنوز فعال نیست؛ لینک را با Import Clipboard وارد کن.');
    else if (id.id === 'addFeedBtn') {
      const input = document.getElementById('subInput');
      Android.addSubscription(input ? input.value.trim() : '');
    }
    else if (id.id === 'syncNowBtn') Android.refreshSubscriptions();
    else if (id.id === 'btn-restart-core') Android.restartVpn();
  }, true);

  document.addEventListener('DOMContentLoaded', function () {
    if (native()) {
      renderProfiles();
      window.vpnState(Android.getStatus(), '');
    }
    attachSettingsShortcuts();
  });

  window.VPNSOPR = {
    onPageReady: function () {
      if (native()) {
        renderProfiles();
        window.vpnState(Android.getStatus(), '');
      }
      attachSettingsShortcuts();
    },
    profileImportResult: function (ok, message) {
      toast(message || (ok ? 'Profile imported' : 'Import failed'));
      if (ok) renderProfiles();
    },
    subscriptionResult: function (ok, message) {
      toast(message || (ok ? 'Subscription imported' : 'Subscription failed'));
      if (ok) {
        const input = document.getElementById('subInput');
        if (input) input.value = '';
        renderProfiles();
      }
    }
  };
})();
