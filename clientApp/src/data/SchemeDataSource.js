import { isvalid, nvl, numberWithCommas } from '../grid/common.js';



/**
 * props: title, tables: table array(type, name, scheme, description)
 */
class SchemeDataSource {
	static _columns_ = ['T', 'Name', 'Scheme', 'Description'];
	
	// { title, tables }
  constructor (props) {
		this.props = props;
  }

  getTitle = () => {
    return this.props.title;
  }

  getColumnCount = () => {
    return SchemeDataSource._columns_.length;
  }

  getColumnName = (col) => {
    return SchemeDataSource._columns_[col];
  }

  getColumnType = (col) => {
		return 'string';
  }

  getPreferedColumnWidth = (c) => {
    const letterWidth = 12 * 8.5 / 16; // 32
    const gValue = '' + this.getCellValue(c, 0);

    return Math.ceil( Math.max(50, Math.max(gValue.length, this.getColumnName(c).length) * letterWidth + 16) );
  }

  getRowCount = () => {
    return this.props.tables.length;
  }

  getRowHeight = () => {
    return 26;
  }

  getCellValue = (col, row) => {
		const rec = this.props.tables[row];

		if( col === 0 ) {
			return rec['type']; // .substring(0, 1);
		} else if( col == 1 ) {
			return rec['name'];
		} else if( col === 2 ) { // scheme
			return rec['scheme'];
		} else if( col == 3 ) {
			return rec['description'];
		}

    return '';
  }

  // eslint-disable-next-line
  isValid = (begin, end) => {
    return true;
  }
}

export default SchemeDataSource;
export { SchemeDataSource };
