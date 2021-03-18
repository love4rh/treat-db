import { isvalid, nvl, numberWithCommas } from './common.js';



class DataSource {
  constructor (props) {
    this.props = props;
    this.colCount = props.columnCount;
    this.rowCount = props.rowCount;
    this.now = new Date();
  }

  getTitle = () => {
    return this.props.title;
  }

  getColumnCount = () => {
    return this.colCount;
  }

  getColumnName = (col) => {
    return 'Column' + col;
  }

  getColumnType = (col) => {
    if( col === 1)
      return 'number';
    else if( col === 2 )
      return 'datetime';

    return 'string';
  }

  getPreferedColumnWidth = (c) => {
    const letterWidth = 12 * 8.5 / 16; // 32
    const gValue = '' + this.getCellValue(c, 0);

    return Math.ceil( Math.max(50, Math.max(gValue.length, this.getColumnName(c).length) * letterWidth + 16) );
  }

  getRowCount = () => {
    return this.rowCount;
  }

  getRowHeight = () => {
    return 26; // nvl(this.props.rowHeight, 30);
  }

  getCellValue = (col, row) => {
    if( col === 1 ) {
      return row + 1; // (row - 10) * 999 + Math.floor(Math.random() * 100) / 100;
    } else if( col === 2 ) {
      return this.now; // new Date();
    }

    return 'C' + col + 'R' + row;
  }

  // eslint-disable-next-line
  isValid = (begin, end) => {
    return true;
  }
}


class DataSource2 {
  // props: title, columnCount, recordCount, columns, records
  constructor (props) {
    this.props = props;

    // props.columns
    // props.records
    this.colCount = props.columnCount;
    this.rowCount = props.recordCount;

    this.state = {
      records: this.props.records
    };
  }

  getTitle = () => {
    return this.props.title;
  }

  getColumnCount = () => {
    return this.colCount;
  }

  getColumnName = (col) => {
    return this.props.columns[col].name;
  }

  getColumnType = (col) => {
    // unknown, string, integer, real, datetime, boolean
    const type = this.props.columns[col].type;

    if( type === 'Integer' || type === 'Real' ) {
      return 'number';
    }

    return 'string';
  }

  getRowCount = () => {
    return this.rowCount;
  }

  getRowHeight = () => {
    return 30;
  }

  getCellValue = (col, row) => {
    const rec = this.state.records['r' + row];

    if( rec )
      return rec[col];

    /*
    const start = Math.max(0, row - 25);
    this.props.getMore(start, 100, (data) => {
      console.log(data);
    }); // */

    return rec ? rec[col] : null;
  }

  // eslint-disable-next-line
  isValid = (begin, end) => {
    let isOk = true;

    end = Math.min(end, this.props.recordCount - 1);
    for(let i = begin; isOk && i <= end; ++i) {
      isOk = isvalid(this.state.records['r' + i]);
    }

    return isOk;
  }

  getMore = (start, len, cb) => {
    this.props.getMore(Math.max(0, start - len), len * 3, (data) => {
      if( isvalid(data.records) ) {
        this.state.records = data.records;
        if( cb ) cb(true);
      } else {
        // console.log('CHECK', data);
        if( cb ) cb(false);
      }
    })
  }
}


export default DataSource;
export { DataSource, DataSource2 };
