
export const availableLanguages = [{
  code: 'en',
  name: 'English'
// }, {
//   code: 'fr',
//   name: 'French'
// }, {
//   code: 'it',
//   name: 'Italien'
}, {
  code: 'ge',
  name: 'German'
}];

export const defaultLanguage = 'de';
export const sysOptions = {
  systemLanguage: defaultLanguage
};

const host = location.host;
const path = '/';
const protocol = location.protocol;
export const backendUrl = (protocol + '//' + host + path).replace('index.html', '');


export function isNumber(value: string | number): boolean {
  return ((value != null) &&
          (value !== '') &&
          !isNaN(Number(value.toString())));
}

export function toDateString(datestr: string): string {
  let date: Date = new Date();
  if (isNumber(datestr)) {
    // tslint:disable-next-line: radix
    date = new Date(parseInt(datestr));
  } else {
    date = new Date(Date.parse(datestr));
  }
  const d = (`${date.getFullYear().toString()}-${('0' + (date.getMonth() + 1)).slice(-2)}-${('0' + (date.getDate())).slice(-2)}`);
  return d;
}

export function formatCurrentMoment(withSeconds: boolean = false) {
  return formatDate(new Date(), withSeconds);
}

export function formatDate(d: Date, withSeconds: boolean = false) {
  const datestring = ('0' + d.getDate()).slice(-2) + '-' + ('0' + (d.getMonth() + 1)).slice(-2) + '-' +
    d.getFullYear() + ' ' + ('0' + d.getHours()).slice(-2) + ':' + ('0' + d.getMinutes()).slice(-2);
  if (withSeconds) {
    return datestring + ':' + ('0' + d.getSeconds()).slice(-2);
  }
  return datestring;
}

export function guid() {
  function s4() {
    return Math.floor((1 + Math.random()) * 0x10000)
      .toString(16)
      .substring(1);
  }
  return s4() + s4() + '-' + s4() + '-' + s4() + '-' +
    s4() + '-' + s4() + s4() + s4();
}

export function utf8_to_b64( str: string ): string {
  return window.btoa(encodeURIComponent( str ).replace(/%([0-9A-F]{2})/g, (match, p1) => String.fromCodePoint(Number.parseInt(p1, 16))));
}

export function b64_to_utf8( str: string ): string {
  return decodeURIComponent(window.atob( str ).split('').map(c => '%' + ('00' + c.codePointAt(0).toString(16)).slice(-2)).join(''));
}

export function clientID() {
  let clientid = localStorage.getItem('clientid');
  if (!clientid) {
    clientid = guid();
    localStorage.setItem('clientid', clientid);
  }
  return localStorage.getItem('current_username') + ':' + clientid;
}

export const gearMapping = {
  1: "boden.svg",
  2: "pferdpauschen.svg",
  3: "ringe.svg",
  4: "sprung.svg",
  5: "barren.svg",
  6: "reck.svg",
  26: "ringe.svg",
  27: "stufenbarren.svg",
  28: "schwebebalken.svg",
  29: "minitramp.svg",
  30: "minitramp.svg",
  31: "ringe.svg"
};


export const turn10ProgrammNames = [
  "BS", "OS"
];

export function formatDateForApi(date: Date): string {
  const iso = date.toISOString();
  return iso.replace('Z', '+0000');
}

export function parseApiDate(dateStr: string): string {
  return dateStr.substring(0, 10);
}

export function formatDisplayDate(dateStr: string): string {
  return dateStr.substring(0, 10);
}

export function downloadBlob(blob: Blob, filename: string): void {
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  window.URL.revokeObjectURL(url);
}
