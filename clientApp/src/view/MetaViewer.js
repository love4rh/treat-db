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
import TreeView from '@material-ui/lab/TreeView';
import ExpandMoreIcon from '@material-ui/icons/ExpandMore';
import ChevronRightIcon from '@material-ui/icons/ChevronRight';
import TreeItem from '@material-ui/lab/TreeItem';

import SearchIcon from '@material-ui/icons/Search';

import DataGrid from '../grid/DataGrid.js';
import SchemeDataSource from '../data/SchemeDataSource.js';

import { isvalid } from '../util/tool.js';

import './MetaViewer.scss';



const theme = createMuiTheme({
  palette: {
    type: 'dark',
  },
  typography: {
    // In Chinese and Japanese the characters are usually larger,
    // so a smaller fontsize may be appropriate.
    fontSize: 11,
  },
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
      databases,
      dsScheme: new SchemeDataSource({ title: 'TableList', tables: databases[0]['scheme'] })
    };
  }

  componentDidMount() {
    //
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
    this.setState({ selected: idx, filterText: '', dsScheme: ds });
  }

  handleFilterChange = (ev) => {
    const { databases, selected } = this.state;
    const metaList = databases[selected]['scheme'];

    const value = ev.target.value;
    const pattern = new RegExp(value, 'i');

    const ds = new SchemeDataSource({
      title: 'TableList',
      tables: metaList.filter((m, i) => value == '' || pattern.test(m.name) || pattern.test(m.description))
    });

    this.setState({ filterText: value, dsScheme: ds });
  }

  handleLayoutChanged = (from, to) => {
    const { schemeBoxHeight } = this.state;
    this.setState({ schemeBoxHeight: schemeBoxHeight + from - to });
  }

  render() {
    const dividerSize = 4;
    const { clientWidth, schemeBoxHeight, databases, selected, filterText, dsScheme } = this.state;

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
        <DataGrid
            width={clientWidth}
            dataSource={dsScheme}
            showRowNumber={false}
            showColumnNumber={false}
            userBeginRow={0}
          />
        </div>
        <LayoutDivider direction={DividerDirection.horizontal}
          size={dividerSize}
          onLayoutChange={this.handleLayoutChanged}
        />
        <div className="metaDetailaBox">
          Detail
        </div>
      </div>
    );
  }
}

export default MetaViewer;
export { MetaViewer };
