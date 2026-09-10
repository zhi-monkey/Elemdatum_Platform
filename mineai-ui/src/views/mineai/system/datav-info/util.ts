/**
 * 格式化字符串
 * <span class="index" style= "background-color:1981f6;">1</span>  ====>  1
 * @param str
 */
const stringFormat = (str: string) => {
  if (str.includes('span')) {
    const startIndex = str.indexOf('>');
    const endIndex = str.lastIndexOf('<');
    return str.substring(startIndex + 1, endIndex);
  } else {
    return str;
  }
};

export { stringFormat };
