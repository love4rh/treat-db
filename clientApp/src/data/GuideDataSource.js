import { isvalid, nvl, numberWithCommas } from '../grid/common.js';



/**
 * 쿼리한 데이터가 없는 경우를 그리드 표시를 위한 데이터소스
 */
class GuideDataSource {
  constructor (props) {
		this.props = props;
  }

  getTitle = () => {
    return 'Guide';
  }

  getColumnCount = () => {
    return 1;
  }

  getColumnName = (col) => {
    return 'Edit query statement and execute (Shift+Enter)';
  }

  getColumnType = (col) => {
		return 'string';
  }

  getPreferedColumnWidth = (c) => {
    return 400;
  }

  getRowCount = () => {
    return 0;
  }

  getRowHeight = () => {
    return 26;
  }

  getCellValue = (col, row) => {
    return '';
  }

  // eslint-disable-next-line
  isValid = (begin, end) => {
    return true;
  }
}

export default GuideDataSource;
export { GuideDataSource };
