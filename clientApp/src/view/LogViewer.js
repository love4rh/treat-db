import React, { Component } from 'react';
import PropTypes from 'prop-types';
import { apiProxy } from '../util/apiProxy.js';

import { isvalid, showGlobalMessage } from '../util/tool.js';

import DiosDataSource from '../grid/DiosDataSource.js';
import DataGrid from '../grid/DataGrid.js';

import { makeRowFilter } from '../grid/common.js';

import LineMap from '../component/LineMap.js';

import './LogViewer.scss';



const makeColumnFilterData = (names, colors) => {
  const items = names.split(' ');
  const colorList = colors ? colors.split(' ') : null;

  return items.map((s, idx) => {
    return { title:s, color:(colorList && colorList[idx]), selected:true };
  });
};


const makeDataSource = (dataMeta, handleMoreData, category, controller) => {
  const ds = new DiosDataSource({ getMore: handleMoreData, ...dataMeta, controller });

  if( isvalid(category) ) {
    const { logTypes, processNames, contextNames, contextColors, processColors } = category;

    ds.setColumnFilterData(3, makeColumnFilterData(logTypes));
    ds.setColumnFilterData(4, makeColumnFilterData(processNames, processColors));
    ds.setColumnFilterData(5, makeColumnFilterData(contextNames, contextColors));
  }

  return ds;
};


let _rowFilterHandle_ = null;

export const doApplyRowFilter = () => {
  if( _rowFilterHandle_ ) {
    _rowFilterHandle_();
  }
}


class LogViewer extends Component {
  static propTypes = {
    category: PropTypes.object.isRequired,
    dataKey: PropTypes.string.isRequired,
    dataMeta: PropTypes.object.isRequired,
    lineColorMap: PropTypes.array.isRequired
  }

  constructor (props) {
    super(props);

    // dataMeta has initData columnCount, columns, recordCount, records, beginIndex.
    const { category, dataMeta, lineColorMap } = props;

    this.state = {
      ds: makeDataSource(dataMeta, this.getMoreData, category, this),
      lineColorMap: lineColorMap,
      redrawCount: 0,
      grid: { begin: 0, end: 2 },
      userBeginRow: 0,
      rowFilter: null
    };

    _rowFilterHandle_ = this.reapplyRowFilter;
  }

  getMoreData = (start, len, cb) => {
    apiProxy.getMoreData(this.props.dataKey, start, len, (res) => {
      const dt = res.data;
      cb(dt);
    }, (err) => {
      cb({ returnValue: false });
    });
  }

  handleFilterChanged = (idx, filterInfo) => {
    const count = filterInfo.reduce((r, v) => r + (v.selected ? 1 : 0), 0);

    if( count === 0 ) {
      showGlobalMessage('범주 아이템은 적어도 하나 이상 선택되어야 합니다.');
      return false;
    }

    this.reapplyRowFilter();

    return true;
  }

  reapplyRowFilter = () => {
    const { ds } = this.state;

    makeRowFilter(ds, (res) => {
      if( isvalid(res.rowFilter) ) {
        ds.setRowFilter(res.rowFilter);
        this.setState({ rowFilter: res.rowFilter });
      }
    });

    /*
    const selected = filterInfo.filter((v) => v.selected).map((v) => v.title).join(' ');

    apiProxy.filterData(this.props.dataKey, idx - 3, selected, (res) => {
      // console.log('handleCatChanged', 'data', res);
      const dt = res.data;
      if( dt && istrue(dt.returnValue) && isvalid(dt.initData) ) {
        this.state.ds.resetData({ getMore:this.getMoreData, ...dt.initData, controller:this });
        this.setState({ lineColorMap: dt.lineColorMap });
      } else {
        // TODO error message
      }
    }, (err) => {
      console.log('handleFilterChanged', 'error', err);
    }); // */
  }

  handlePositionChanged = (s) => {
    // console.log('handlePositionChanged', s);
    this.setState({ userBeginRow: s });
  }

  handleGridVisAreaChange = (r1, r2) => {
    // console.log('handleGridVisAreaChange', r1, r2);
    this.setState({ grid: { begin: r1, end: r2 } });
  }

  render () {
    const { ds, grid, userBeginRow, lineColorMap, rowFilter } = this.state;

    return (
      <div className="viewerMain">
        <div className="gridBox">
          <DataGrid
            dataSource={ds}
            onVisibleAreaChanged={this.handleGridVisAreaChange}
            userBeginRow={userBeginRow}
            fixableCount={6}
          />
        </div>
        <div className="mapBox">
          <LineMap
            colorMap={lineColorMap}
            beginRow={grid.begin}
            endRow={grid.end}
            rowFilter={rowFilter}
            onPositionChanged={this.handlePositionChanged}
          />
        </div>
      </div>
    );
  }
}

export default LogViewer;
export { LogViewer };
