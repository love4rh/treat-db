import { isundef, istrue, isvalid, numberWithCommas, tickCount } from './common.js';



class DiosDataSource {
  // props: (title), columnCount, recordCount, columns, records, beginIndex, count, getMore, controller
  // columns has elemnts that have {name, type}. type: DateTime, Integer, Real, Text
  // controller: filterChanging
  constructor (props) {
    this.resetData(props);
    this._filterMap = {};
    this._rowFilter = null;  // 사용할 Record의 인덱스를 담고 있는 array. null이면 모든 Record를 의미함
    this._modifiedTime = tickCount();
  }

  resetData = (props) => {
    this.props = props;

    const { records, beginIndex } = props;

    this.state = {
      records: records,
      sIdx: beginIndex,
      eIdx: beginIndex + records.length // (exclusive)
    };

    this._rowFilter = null;
  }

  getTitle = () => {
    return this.props.title;
  }

  getColumnCount = () => {
    return this.props.columnCount;
  }

  getColumnName = (col) => {
    return this.props.columns[col].name;
  }

  getColumnType = (col) => {
    // unknown, string, Integer, Real, DateTime, Text
    const type = this.props.columns[col].type;

    if( type === 'Integer' || type === 'Real' ) {
      return 'number';
    }

    return type;
  }

  setRowFilter = (rowFilter) => {
    this._rowFilter = rowFilter;
  }

  _getRowCount = (raw) => {
    return !raw && this._rowFilter ? this._rowFilter.length : this.props.recordCount;
  }

  getRowCount = () => {
    return this._getRowCount(false);
  }

  getRowHeight = () => {
    return 26;
  }

  pad = (n) => {
    return n < 10 ? '0' + n : '' + n;
  }

  _getRawCellValue = (col, row) => {
    const { records, sIdx } = this.state;
    const rec = records[row - sIdx];

    // console.log('getCellValue', col, row, sIdx, eIdx);

    if( rec ) {
      switch( this.getColumnType(col) ) {
        case 'DateTime':
        {
          const dt = new Date(rec[col]);
          return dt.getUTCFullYear() + '-' + this.pad(dt.getUTCMonth() + 1) + '-' + this.pad(dt.getUTCDate())
            + ' ' + this.pad(dt.getUTCHours()) + ':' + this.pad(dt.getUTCMinutes()) + ':' + this.pad(dt.getUTCSeconds());
        }
        case 'Text':
          return decodeURIComponent(rec[col]).replace(/[+]/g, ' ');
        default:
          return rec[col];
      }
    }

    return null;
  }

  getCellValue = (col, row) => {
    if( this._rowFilter ) {
      row = this._rowFilter[row];
    }

    return this._getRawCellValue(col, row);
  }

  applyColumnFilter = (selectedItems) => {
    for(let c = 0; c < selectedItems.length; ++c) {
      let cf = this._filterMap['col' + c];
      if( isundef(selectedItems[c]) || isundef(cf) ) {
        continue;
      }

      const dict = selectedItems[c];

      let fd = cf.filterData;
      for(let i = 0; i < fd.length; ++i) {
        fd[i].selected = istrue(dict[fd[i].title]);
      }

      this._filterMap['col' + c] = { filterData:fd, selectedCount:fd.reduce((r, v) => r + (v.selected ? 1 : 0), 0) };
    }
  }

  // filterData : [{title, selected(, color)}, ...]
  setColumnFilterData = (col, filterData) => {
    const cList = this._filterMap['col' + col];

    if( isundef(cList) ) {
      this._filterMap['col' + col] = { filterData, selectedCount:filterData.length };
      return;
    }

    if( 0 === cList.filterData.reduce((res, val, i) => res + (filterData[i].selected !== val.selected ? 1 : 0), 0) ) {
      return;
    }

    // console.log('setColumnFilterData', col, filterData);
    // console.log('setColumnFilterData', col, filterData.filter((d) => d.selected).reduce((m, d) => ({...m, [d.title]:true}), {}) );

    const rb = this._filterMap['col' + col];
    const { controller } = this.props;

    this._filterMap['col' + col] = { filterData, selectedCount:filterData.reduce((r, v) => r + (v.selected ? 1 : 0), 0) };

    if( controller && controller.handleFilterChanged && !controller.handleFilterChanged(col, filterData)) {
      this._filterMap['col' + col] = rb;
    }

    this._modifiedTime = tickCount();
  }

  hasColumnFilterData = (col) => {
    return ('col' + col) in this._filterMap;
  }

  getColumnFilterData = (col) => {
    const cf = this._filterMap['col' + col];
    return cf ? cf.filterData : null;
  }

  isColumnFiltered = (col) => {
    const cf = this._filterMap['col' + col];
    if( isundef(cf) ) {
      return false;
    }
    return cf.filterData.length > cf.selectedCount;
  }

  getPreferedColumnWidth = (c) => {
    const letterWidth = 12 * 8.5 / 16; // 32

    const { sIdx, eIdx } = this.state;
    let w = Math.max(50, this.getColumnName(c).length * letterWidth + 16); // minimum size of column

    for(let r = 0; r < Math.min(20, eIdx - sIdx); ++r) {
      const val = this.getCellValue(c, r + sIdx);

      if( isvalid(val) ) {
        if( 'number' === this.getColumnType(c) && typeof val === 'number' ) {
          w = Math.max(w, numberWithCommas(val).length * letterWidth + 16);
        } else {
          w = Math.max(w, ('' + val).length * letterWidth + 16);
        }
      }
    }

    return Math.ceil(w) + (this.hasColumnFilterData && this.hasColumnFilterData(c) ? this.getRowHeight() : 0);
  }

  // eslint-disable-next-line
  isValid = (begin, end) => {
    const { sIdx, eIdx } = this.state;
    end = Math.min(end, this.getRowCount() - 1);
    return sIdx <= begin && begin <= end && end < eIdx;
  }

  getMore = (start, len, cb) => {
    this.props.getMore(Math.max(0, start - len), len * 4, (data) => {
      if( isvalid(data.records) ) {
        const { records, beginIndex } = data;
        this.state = {
          records: records,
          sIdx: beginIndex,
          eIdx: beginIndex + records.length // (not inclusive)
        };
        if( cb ) cb(true);
      } else {
        // console.log('CHECK', data);
        if( cb ) cb(false);
      }
    });
  }
};


export default DiosDataSource;
export { DiosDataSource };
