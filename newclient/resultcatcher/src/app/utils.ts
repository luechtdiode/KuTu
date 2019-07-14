
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

export function clientID() {
  let clientid = localStorage.getItem('clientid');
  if (!clientid) {
    clientid = guid();
    localStorage.setItem('clientid', clientid);
  }
  return localStorage.getItem('current_username') + ':' + clientid;
}
