import { mainNavigationItems } from './navigation';

const chinesePattern = /[\u3400-\u9fff]/;

for (const item of mainNavigationItems) {
  if (chinesePattern.test(item.label)) {
    throw new Error(`main menu label should be English only: ${item.label}`);
  }
}
