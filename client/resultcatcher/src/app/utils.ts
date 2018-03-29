
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
const path = location.pathname;
const protocol = location.protocol;
export const backendUrl = protocol + "//" + host + path;

export function formatCurrentMoment(withSeconds: boolean = false) {
  return formatDate(new Date(), withSeconds);
}

export function formatDate(d: Date, withSeconds: boolean = false) {
  const datestring = ("0" + d.getDate()).slice(-2) + "-" + ("0" + (d.getMonth() + 1)).slice(-2) + "-" +
    d.getFullYear() + " " + ("0" + d.getHours()).slice(-2) + ":" + ("0" + d.getMinutes()).slice(-2);
  if (withSeconds) {
    return datestring + ":" + ("0" + d.getSeconds()).slice(-2);
  }
  return datestring;
}