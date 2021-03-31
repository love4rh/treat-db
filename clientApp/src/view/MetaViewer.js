import React, { Component } from 'react';
import PropTypes from 'prop-types';

import { LayoutDivider, DividerDirection } from '../component/LayoutDivider.js';

import { createMuiTheme, ThemeProvider } from '@material-ui/core/styles';

import InputLabel from '@material-ui/core/InputLabel';
import Input from '@material-ui/core/Input';
import InputAdornment from '@material-ui/core/InputAdornment';
import MenuItem from '@material-ui/core/MenuItem';
import FormControl from '@material-ui/core/FormControl';
import Select from '@material-ui/core/Select';

import SearchIcon from '@material-ui/icons/Search';

import { DataGrid, GridEvent } from '../grid/DataGrid.js';

import SchemeDataSource from '../data/SchemeDataSource.js';
import ColumnDataSource from '../data/ColumnDataSource.js';

import { isvalid } from '../util/tool.js';
import { Log } from '../util/Logging.js';
import { AppData } from '../data/AppData.js';

import './MetaViewer.scss';



const theme = createMuiTheme({
  palette: {
    type: 'dark',
  },
  typography: {
    // In Chinese and Japanese the characters are usually larger,
    // so a smaller fontsize may be appropriate.
    fontSize: 11,
  }
});


class MetaViewer extends Component {
  static propTypes = {
    width: PropTypes.number,
    height: PropTypes.number,
    databases: PropTypes.array,
  }

  constructor (props) {
    super(props);

    const { databases } = props;

    this.state = {
      clientWidth: props.width,
      clientHeight: props.height,
      schemeBoxHeight: 250,
      filterText: '',
      selected: 0,
      tableShown: -1,
      databases,
      dsScheme: new SchemeDataSource({ title: 'TableList', tables: databases[0]['scheme'] }),
      dsColumn: null,
      detailTab: 0 // 0: Columns, 1: Index
    };
  }

  componentDidMount() {
    this.showTableColumn(0);
  }

  componentWillUnmount() {
    //
  }

  static getDerivedStateFromProps(nextProps, prevState) {
    if( prevState.clientWidth !== nextProps.width || prevState.clientHeight !== nextProps.height ) {
      return { clientWidth: nextProps.width, clientHeight: nextProps.height };
    }

    return null;
  }

  handleDBChange = (ev) => {
    const idx = ev.target.value;
    const { databases } = this.state;
    const ds = new SchemeDataSource({ title: 'TableList', tables: databases[idx]['scheme'] });

    this.setState({ selected: idx, filterText: '', dsScheme: ds, tableShown: 0 });
    AppData.setDatabase(idx);

    Log.i('Database changed to "' + databases[idx]['name'] + '"');
  }

  handleFilterChange = (ev) => {
    const { databases, selected } = this.state;
    const metaList = databases[selected]['scheme'];

    const value = ev.target.value;
    const pattern = new RegExp(value, 'i');
    const tables = metaList.filter((m) => value === '' || pattern.test(m.name) || pattern.test(m.description));

    console.log('handleFilterChange', tables);

    this.setState({ filterText: value, dsScheme: new SchemeDataSource({ title: 'TableList', tables }) });
  }

  handleLayoutChanged = (from, to) => {
    const { schemeBoxHeight } = this.state;
    this.setState({ schemeBoxHeight: schemeBoxHeight + from - to });
  }

  handleSchemeGridEvent = (eventType, option) => {
    // console.log('handleSchemeGrid', eventType, option);
    if( eventType === GridEvent.CELL_SELECTED && isvalid(option) ) {
      this.showTableColumn(option.row);
    }
  }

  showTableColumn = (idx) => {
    const { tableShown, dsScheme } = this.state;
    if( tableShown !== idx ) {
      const ds = new ColumnDataSource({ title: 'ColumnList', columns: dsScheme.getColumnsData(idx) })
      this.setState({ tableShown: idx, dsColumn: ds });
    }
  }

  handleColumnGridEvent = (eventType, option) => {
    // console.log('handleColumnGridEvent', eventType, option);
  }

  handleTabChange = (ev, newTab) => {
    this.setState({ detailTab: newTab });
  }

  render() {
    const dividerSize = 4, optionBoxHeight = 98;;
    const { clientHeight, clientWidth, schemeBoxHeight, databases, selected, filterText, dsScheme, dsColumn, detailTab } = this.state;
    const columnBoxHeight = clientHeight - dividerSize - optionBoxHeight - schemeBoxHeight;

    return (
      <div className="metaBox" style={{ width:clientWidth }}>
        <div className="dbSelectBox">
          <ThemeProvider theme={theme}>
            <FormControl className="dbSelectDiv">
              <InputLabel id="db-select-label">Database</InputLabel>
              <Select
                labelId="db-select-label"
                id="db-select"
                value={selected}
                onChange={this.handleDBChange}
              >
                { databases.map((o, i) => <MenuItem key={`dbopt-${i}`} value={i}>{o.name}</MenuItem>) }
              </Select>
            </FormControl>
            <div className="separatorDiv">&nbsp;</div>
            <FormControl className="schemeFilterDiv">
              <Input
                id="scheme-filter-text"
                startAdornment={
                  <InputAdornment position="start">
                    <SearchIcon />
                  </InputAdornment>
                }
                value={filterText}
                onChange={this.handleFilterChange}
              />
            </FormControl>
          </ThemeProvider>
          <div className="separatorDiv">&nbsp;</div>
        </div>
        <div className="metaDataBox" style={{ flexBasis:schemeBoxHeight }}>
          { isvalid(dsScheme) &&
            <DataGrid
              height={schemeBoxHeight}
              width={clientWidth}
              dataSource={dsScheme}
              showRowNumber={false}
              showColumnNumber={false}
              userBeginRow={0}
              onEvent={this.handleSchemeGridEvent}
            />
          }
        </div>
        <LayoutDivider direction={DividerDirection.horizontal}
          size={dividerSize}
          onLayoutChange={this.handleLayoutChanged}
        />
        <div className="metaDetailaBox">
          { isvalid(dsColumn) && detailTab === 0 &&
            <DataGrid
              height={columnBoxHeight}
              width={clientWidth}
              dataSource={dsColumn}
              showColumnNumber={false}
              userBeginRow={0}
              onEvent={this.handleColumnGridEvent}
            />
          }
          { detailTab === 1 &&
            <div>implementing...</div>
          }
        </div>
      </div>
    );
  }
}

export default MetaViewer;
export { MetaViewer };
