import { withInstall } from '/@/utils';
import formSelectItem from './src/FormSelectItem.vue';

export const FormSelectItem = withInstall(formSelectItem);
export interface itemType {
  value: string;
  label: string;
  disabled?: boolean;
}
