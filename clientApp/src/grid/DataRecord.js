import React, { Component } from 'react';
import PropTypes from 'prop-types';

import { DataCell } from './DataCell.js';

import './styles.scss';



class DataRecord extends Component {
  static propTypes = {
    ds: PropTypes.object.isRequired,
    getColumnWidth: PropTypes.func.isRequired,
    row: PropTypes.number.isRequired,
    changeState: PropTypes.func,
    isSelected: PropTypes.func,
    doHoldEvent: PropTypes.func,
    fixedColumn: PropTypes.number
  }

  render () {
    const { ds, row, isSelected, getColumnWidth, doHoldEvent, fixedColumn } = this.props;

    let left = 0;
    let tagList = [];
    const colCount = ds.getColumnCount();
    const rowHeight = ds.getRowHeight();
    const lineHeight = (rowHeight - 10) + 'px';

    for(let c = fixedColumn; c < colCount; ++c) {
      const width = getColumnWidth(c);
      const cellValue = ds.getCellValue(c, row);

      // if( c === 0 && typeof cellValue === 'object' ) {
      //   console.log('DataRecord render', cellValue);
      // }

      tagList.push(
        <DataCell
          key={`c-r${row}-c${c}`}
          type={ds.getColumnType(c)}
          selected={isSelected(c, row)}
          width={width}
          height={rowHeight}
          lineHeight={lineHeight}
          value={cellValue}
          doHoldEvent={doHoldEvent}
          col={c}
          row={row}
        >
          { cellValue }
        </DataCell>
      );

      left += width;
    }

    return (
      <div className="dataCells" style={{ height:rowHeight, width:left }}>
        {tagList.map((tag) => tag)}
      </div>
    );
  }
}


export default DataRecord;
export { DataRecord };
