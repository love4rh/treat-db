import React, { Component } from 'react';
import PropTypes from 'prop-types';

import cn from 'classnames';
import { numberWithCommas } from './common.js';
import { DataCell } from './DataCell.js';

import './styles.scss';



class RowHeader extends Component {
  static propTypes = {
    ds: PropTypes.object,
    changeState: PropTypes.func,
    index: PropTypes.number,
    selected: PropTypes.bool,
    fixedColumn: PropTypes.number,
    getColumnWidth: PropTypes.func,
    doHoldEvent: PropTypes.func,
    isSelected: PropTypes.func,
    headerWidth: PropTypes.number,
  }

  render () {
    const { index, ds, selected, fixedColumn, getColumnWidth, doHoldEvent, isSelected, headerWidth } = this.props;
    const rowHeight = ds.getRowHeight();
    const lineHeight = (rowHeight - 10) + 'px';

    const tagList = [];

    for(let c = 0; c < fixedColumn; ++c) {
      const width = getColumnWidth(c);

      tagList.push(
        <DataCell
          key={`fc-r${index}-c${c}`}
          type={ds.getColumnType(c)}
          selected={isSelected(c, index)}
          width={width}
          height={rowHeight}
          lineHeight={lineHeight}
          value={ds.getCellValue(c, index)}
          doHoldEvent={doHoldEvent}
          col={c}
          row={index}
          fixedType={c === fixedColumn - 1 ? 2 : 1}
        />
      );
    }

    return (
      <div className="rowHeaderLine">
        <div className={cn({ 'rowCell': true, 'selectedHeader': selected })}
          style={{ width:headerWidth, height:rowHeight, lineHeight:lineHeight, flextBasis:headerWidth }}
        >
          {numberWithCommas(index + 1)}
        </div>
        { tagList.map((t) => t) }
      </div>
    );
  }
}


export default RowHeader;
export { RowHeader };
