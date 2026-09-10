import JSEncrypt from 'jsencrypt/bin/jsencrypt';

const publicKey =
  'MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBANL378k3RiZHWx5AfJqdH9xRNBmD9wGD2iRe41HdTNF8RUhNnHit5NpMNtGL0NPTSSpPjjI1kJfVorRvaQerUgkCAwEAAQ==';

// 加密
export function encrypt(txt) {
  const encryptor = new JSEncrypt();
  encryptor.setPublicKey(publicKey);
  return encryptor.encrypt(txt);
}

// 解密
export function decrypt(txt, privateKey) {
  const encryptor = new JSEncrypt();
  encryptor.setPrivateKey(privateKey);
  return encryptor.decrypt(txt);
}
