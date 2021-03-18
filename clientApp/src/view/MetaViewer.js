import React, { Component } from 'react';
import PropTypes from 'prop-types';

import { makeStyles } from '@material-ui/core/styles';
import { createMuiTheme, ThemeProvider } from '@material-ui/core/styles';

import TreeView from '@material-ui/lab/TreeView';
import ExpandMoreIcon from '@material-ui/icons/ExpandMore';
import ChevronRightIcon from '@material-ui/icons/ChevronRight';
import TreeItem from '@material-ui/lab/TreeItem';

import './MetaViewer.scss';


const theme = createMuiTheme({
  typography: {
    // In Chinese and Japanese the characters are usually larger,
    // so a smaller fontsize may be appropriate.
    fontSize: 12,
  },
});


class MetaViewer extends Component {
  static propTypes = {
    width: PropTypes.number,
    height: PropTypes.number,
  }

  constructor (props) {
    super(props);

    const data = {
      id: 'root',
      name: 'Parent',
      children: [
        {
          id: '1',
          name: 'Child - 1',
        },
        {
          id: '3',
          name: 'Child - 3',
          children: [
            {
              id: '4',
              name: 'Child - 4',
            },
          ],
        },
      ],
    };

    this.state = {
      clientWidth: props.width,
      clientHeight: props.height,
      data
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

  renderTree = (nodes) => (
    <TreeItem key={nodes.id} nodeId={nodes.id} label={nodes.name}>
      {Array.isArray(nodes.children) ? nodes.children.map((node) => this.renderTree(node)) : null}
    </TreeItem>
  );

  render() {
    const { clientWidth, clientHeight, data } = this.state;

    const headerSize = 32;

    return (
      <div className="metaBox">
        <div className="dbSelectBox" style={{ height:headerSize }}>
          Database
        </div>
        <div className="metaDataBox" style={{ height:(clientHeight - headerSize) }}>
          <ThemeProvider theme={theme}>
            <TreeView
              className="metaTreeBox"
              defaultCollapseIcon={<ExpandMoreIcon />}
              defaultExpandIcon={<ChevronRightIcon />}
              defaultExpanded={['root']}
            >
              { this.renderTree(data) }
            </TreeView>
          </ThemeProvider>
        </div>
      </div>
    );
  }
}

export default MetaViewer;
export { MetaViewer };
