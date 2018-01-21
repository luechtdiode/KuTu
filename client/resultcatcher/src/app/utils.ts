
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
