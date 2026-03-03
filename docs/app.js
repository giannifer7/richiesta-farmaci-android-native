'use strict';

const DEFAULT_TEMPLATE = 'Farmaci: {COGNOME} {nome}, {lista(", ", {qta} x {farmaco})}';

function getPrefs() {
    return {
        patient_firstname:  localStorage.getItem('patient_firstname')  || '',
        patient_lastname:   localStorage.getItem('patient_lastname')   || '',
        fiscal_code:        localStorage.getItem('fiscal_code')        || '',
        doctor_phone:       localStorage.getItem('doctor_phone')       || '',
        doctor_email:       localStorage.getItem('doctor_email')       || '',
        send_method:        localStorage.getItem('send_method')        || 'whatsapp',
        message_template:   localStorage.getItem('message_template'),
    };
}

function getMedicinesText() {
    return localStorage.getItem('medicines_text') || '[x] 1 x Paracetamolo 1000 mg\n';
}

function parseMedicines() {
    return parseImportText(getMedicinesText());
}

function parseImportText(text) {
    const trimmed = (text || '').trim();
    if (!trimmed) return [];
    if (/^farmaci:/i.test(trimmed)) {
        const parts = trimmed.slice(trimmed.indexOf(':') + 1).split(',').map(s => s.trim()).filter(Boolean);
        return parts.slice(1).map(s => ({ entry: s, isDefault: false }));
    }
    return trimmed.split('\n').map(line => {
        const t = line.trim();
        if (!t) return null;
        if (/^\[x\]\s/i.test(t)) return { entry: t.slice(4).trim(), isDefault: true };
        if (t.startsWith('[ ] '))  return { entry: t.slice(4).trim(), isDefault: false };
        return { entry: t, isDefault: false };
    }).filter(x => x && x.entry);
}

function medicineNameOnly(entry) {
    return entry.replace(/^\d+\s*x\s+/i, '').trim();
}

function applyTemplate(template, firstName, lastName, fiscalCode, selectedMeds) {
    const listaRegex = /\{lista\("([^"]*)",\s*([^)]*)\)\}/g;
    let result = template.replace(listaRegex, (_, rawSep, itemTpl) => {
        const sep = rawSep.replace(/\\n/g, '\n').replace(/\\t/g, '\t').replace(/\\\\/g, '\\');
        return selectedMeds.map(med => {
            const m = med.trim().match(/^(\d+)\s*x\s+(.+)/i);
            const qty  = m ? m[1] : '1';
            const name = m ? m[2].trim() : med.trim();
            return itemTpl.replace(/{qta}/g, qty).replace(/{farmaco}/g, name);
        }).join(sep);
    });
    return result
        .replace(/{COGNOME}/g, lastName.toUpperCase())
        .replace(/{cognome}/g, lastName)
        .replace(/{nome}/g,    firstName)
        .replace(/{cod_fisc}/g, fiscalCode);
}

function formatLastRequest() {
    const ms = parseInt(localStorage.getItem('last_request_ms') || '0');
    if (!ms) return '';
    const sentDate = new Date(ms);
    const today    = new Date(); today.setHours(0,0,0,0);
    const sentDay  = new Date(sentDate); sentDay.setHours(0,0,0,0);
    const days     = Math.round((today - sentDay) / 86400000);
    const dateLabel = sentDate.toLocaleDateString('it-IT', { day: 'numeric', month: 'long', year: 'numeric' });
    const daysLabel = days === 0 ? 'oggi' : days === 1 ? '1 giorno fa' : days + ' giorni fa';
    return 'Ultima richiesta ' + daysLabel + ' (' + dateLabel + ')';
}

if ('serviceWorker' in navigator) {
    navigator.serviceWorker.register('sw.js');
}
