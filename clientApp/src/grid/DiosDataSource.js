import { isundef, istrue, isvalid, numberWithCommas, dateToString, tickCount, nvl } from './common.js';



class DiosDataSource {
  // props: { (title), columnCount, recordCount, columns, records, beginIndex, getMore, controller, (fetchDone) }
  // columns has elemnts that have {name, type}. type: DateTime, Integer, Real, Text
  // controller: filterChanging
  // fetchDone: 비동기 방식으로 데이터를 가져오는 경우 모두 가져왔는지 여부를 알아야 할 필요가 있어 추가함.
  // 이 값이 없으면 모두 가져 온 것으로 판단하고 값이 있고 false이면 주기적으로 getMore를 호출하여 확인하도록 하자.
  constructor (props) {
    this.resetData(props);

    this._filterMap = {};
    this._rowFilter = null;  // 사용할 Record의 인덱스를 담고 있는 array. null이면 모든 Record를 의미함
    this._modifiedTime = tickCount();

    const { getMore, fetchDone } = props;

    if( isvalid(fetchDone) && !istrue(fetchDone) && isvalid(getMore) ) {
      this._fetchDoneChecker = setTimeout(this.checkFetchDone, 1000);
    }
  }

  resetData = (props) => {
    this.props = props;

    const { columns, recordCount, records, beginIndex } = props;

    this.state = {
      columns,
      recordCount,
      records: records,
      sIdx: beginIndex,
      eIdx: beginIndex + records.length // (exclusive)
    };

    this._rowFilter = null;
  }

  getTitle = () => {
    return this.props.title;
  }

  updatedTime = () => {
    return this._modifiedTime;
  }

  getColumnCount = () => {
    return this.state.columns.length;
  }

  getColumnName = (col) => {
    return this.state.columns[col].name;
  }

  getColumnType = (col) => {
    // unknown, string, Integer, Real, DateTime, Text
    const type = this.state.columns[col].type;

    if( type === 'Integer' || type === 'Real' ) {
      return 'number';
    }

    return type;
  }

  setRowFilter = (rowFilter) => {
    this._rowFilter = rowFilter;
  }

  _getRowCount = (raw) => {
    return !raw && this._rowFilter ? this._rowFilter.length : this.state.recordCount;
  }

  getRowCount = () => {
    return this._getRowCount(false);
  }

  getRowHeight = () => {
    return 26;
  }

  _getRawCellValue = (col, row) => {
    const { records, sIdx } = this.state;
    const rec = records[row - sIdx];

    if( rec ) {
      if( isundef(rec[col]) ) {
        return null;
      }

      switch( this.getColumnType(col) ) {
        case 'DateTime':
          return dateToString(new Date(rec[col]), false);
        case 'Text':
          return decodeURIComponent(rec[col]).replace(/[+]/g, ' ');
        default:
          return nvl(rec[col], '');
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
    const letterWidth = 7.5;

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
    this.props.getMore(Math.max(0, start - len), len * 4,
      (data) => {
        if( isvalid(data.records) ) {
          const { fetchDone, records, beginIndex, recordCount } = data;

          this.state.records = records;
          this.state.sIdx = beginIndex;
          this.state.eIdx = beginIndex + records.length; // (not inclusive)

          if( isvalid(recordCount) ) {
            this.props.recordCount = recordCount;
            this.state.recordCount = recordCount;
            this._modifiedTime = tickCount();
          }

          if( isundef(fetchDone) || istrue(fetchDone) ) {
            this.props.fetchDone = true;
          }

          if( cb ) cb(true);
        } else {
          if( cb ) cb(false);
        }
      }
    );
  }

  checkFetchDone = () => {
    const { recordCount } = this.props;

    this.props.getMore(recordCount, 1,
      (data) => {
        const { fetchDone, recordCount } = data;

        if( isvalid(recordCount) ) {
          this.props.recordCount = recordCount;
          this.state.recordCount = recordCount;
          this._modifiedTime = tickCount();
        }

        if( isvalid(fetchDone) && !istrue(fetchDone) ) {
          this._fetchDoneChecker = setTimeout(this.checkFetchDone, 3000);
        } else {
          this.props.fetchDone = true;
        }
      }
    );
  }
};


export default DiosDataSource;
export { DiosDataSource };
