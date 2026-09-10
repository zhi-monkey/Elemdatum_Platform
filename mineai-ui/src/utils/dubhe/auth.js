import Cookies from 'js-cookie';

const TokenKey = 'DUBHE-ADMIN-TOKEN';
const UserInfoKey = 'DUBHE-USER-INFO';

export function getTokenFromCookie() {
  return Cookies.get(TokenKey);
}

export function setTokenToCookie(token) {
  return Cookies.set(TokenKey, token, { expires: 1 });
}

export function setUserInfoToCookie(token) {
  return Cookies.set(UserInfoKey, token, { expires: 1 });
}

export function getUserInfoFromCookie() {
  return Cookies.get(UserInfoKey);
}

export function removeToken() {
  return Cookies.remove(TokenKey);
}
