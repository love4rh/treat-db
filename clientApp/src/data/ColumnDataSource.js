import { isvalid, nvl, numberWithCommas } from '../grid/common.js';



/**
 * props: title, column(name, type, nullable, description)
 */
class ColumnDataSource {
  static _columns_ = ['T', 'Name', 'Description', 'Null'];
  static _columnsWidth_ = [30, 150, 150, 30];
	
	// { title, columns }
  constructor (props) {
		this.props = props;
  }

  getTitle = () => {
    return this.props.title;
  }

  getColumnCount = () => {
    return ColumnDataSource._columns_.length;
  }

  getColumnName = (col) => {
    return ColumnDataSource._columns_[col];
  }

  getColumnType = (col) => {
		return 'string';
  }

  getPreferedColumnWidth = (c) => {
    if( c === 0 || c === 3 ) {
      return 30;
    }

    const letterWidth = 12 * 8.5 / 16; // 32
    const gValue = '' + this.getCellValue(c, 0);

    return Math.max(ColumnDataSource._columnsWidth_[c], Math.ceil( Math.max(50, Math.max(gValue.length, this.getColumnName(c).length) * letterWidth + 16) ) );
  }

  getRowCount = () => {
    return this.props && this.props.columns ? this.props.columns.length : 0;
  }

  getRowHeight = () => {
    return 26;
  }

  getCellValue = (col, row) => {
    if( !this.props || !this.props.columns ) {
      return '';
    }

		const rec = this.props.columns[row];

    if( !rec ) {
      return '';
    }

		if( col === 0 ) {
			return rec['type']; // .substring(0, 1);
		} else if( col == 1 ) {
			return rec['name'];
		} else if( col === 2 ) {
			return rec['description'];
		} else if( col == 3 ) {
      return rec['nullable'];
		}

    return '';
  }

  // eslint-disable-next-line
  isValid = (begin, end) => {
    return true;
  }
}

export default ColumnDataSource;
export { ColumnDataSource };
