import React, { Component } from 'react';

import cn from 'classnames';

import { isundef, isvalid, istrue, nvl, setGlobalMessageHandle } from '../util/tool.js';

import { getCurrentActiveGrid } from '../grid/common.js';

import { apiProxy, _serverBaseUrl_ } from '../util/apiProxy.js';

import { BsList } from 'react-icons/bs';
import { RiArrowGoBackFill, RiFileDownloadLine } from 'react-icons/ri';
// import { FiRefreshCw } from "react-icons/fi";

import Spinner from 'react-bootstrap/Spinner'
import Toast from 'react-bootstrap/Toast'

import TextInputView from '../view/TextInputView.js';
import { LogViewer, doApplyRowFilter } from '../view/LogViewer.js';

import './MainFrame.scss';



class MainFrame extends Component {
  constructor (props) {
    super(props);

    const logUrl = localStorage.getItem('lastUrl');
    const stateOpt = localStorage.getItem('options');

    this.state = {
      pageType: 'entry', // list, edit
      message: null,
      waiting: false,
      menuShown: false,
      urlLog: nvl(logUrl, ''),
      dataKey: null,
      inputOn: false,
      redrawCount: 0,
      savedOptions: isvalid(stateOpt) ? JSON.parse(stateOpt) : []
    };

    this.handleUnload = this.handleUnload.bind(this);
  }

  componentDidMount() {
    // window.addEventListener('beforeunload', this.handleUnload);
    setGlobalMessageHandle(this.showInstanceMessage);
    apiProxy.setWaitHandle(this.enterWaiting, this.leaveWaiting);
  }

  componentWillUnmount() {
    // window.removeEventListener('beforeunload', this.handleUnload);
  }

  handleUnload = (ev) => {
    const message = 'Are you sure you want to close?';

    ev.preventDefault();
    (ev || window.event).returnValue = message;

    return message;
  }

  showInstanceMessage = (msg) => {
    // console.log('showInstanceMessage', msg);
    this.setState({ waiting: false, message: msg });
  }

  enterWaiting = () => {
    this.setState({ waiting: true });
  }

  leaveWaiting = () => {
    this.setState({ waiting: false });
  }

  handleUrlChanged = (logUrl) => {
    if( isundef(logUrl) ) {
      this.showInstanceMessage('invalid URL.');
      return;
    }

    logUrl = logUrl.trim();
    if( logUrl === '' ) {
      this.showInstanceMessage('invalid URL: [' + logUrl + ']');
      return;
    }

    localStorage.setItem('lastUrl', logUrl);

    this.refreshData(logUrl);
  }

  handleRefresh = () => {
    this.refreshData(this.state.urlLog);
  }

  refreshData = (logUrl) => {
    if( isundef(logUrl) ) {
      return;
    }

    this.setState({ waiting: true, redrawCount: 0 });

    apiProxy.go(logUrl, (res) => {
      const dt = res.data;
      let { redrawCount } = this.state;

      console.log('GO Analysis', redrawCount, res);

      if( isundef(dt) ) {
        this.showInstanceMessage('Timeout or error occurred.');
      } else if( dt.code === 'ERROR' ) {
        this.showInstanceMessage('Invalid URL');
      } else if( istrue(dt.returnValue) ) {
        this.setState({
          pageType: 'list',
          dataKey: dt.dataKey,
          dataMeta: dt.initData,
          category: dt.category,
          lineColorMap: dt.lineColorMap,
          urlLog: logUrl,
          waiting: false,
          redrawCount: 0
        });
      } else if( redrawCount > 7 ) {
        this.showInstanceMessage('Can not analysis the log url.');
      } else {
        this.setState({ redrawCount: redrawCount + 1 });
        setTimeout(() => {
          this.refreshData(logUrl);
        }, 2000);
      }
    }, (err) => {
      console.log('GO Analysis: Error', err);
      this.showInstanceMessage('Error occurred: ' + err);
    });
  }

  handleMenu = () => {
    const { menuShown } = this.state;
    this.setState({ menuShown: !menuShown });
  }

  handleClickMenu = (type) => (ev) => {
    ev.preventDefault();
    ev.stopPropagation();

    const { pageType } = this.state;

    if( type === 'entry' ) {
      this.setState({ pageType:type, menuShown:false });
    } else if( type === 'save' && pageType === 'list' ) {
      // 현재 그리드 상태 저장
      // 컬럼 너비, 필터링 옵션
      this.setState({ inputOn:true, menuShown:false });
    } else {
      this.setState({ menuShown:false, inputOn:false });
    }
  }

  hideToastShow = () => {
    this.setState({ message: null });
  }

  handleApplyStatus = (ix) => () => {
    const grid = getCurrentActiveGrid();

    if( isundef(grid) ) {
      return;
    }

    const { savedOptions } = this.state;
    const { dataSource } = grid.props;

    const ds = dataSource;

    ds.applyColumnFilter(savedOptions[ix].itemSelected);

    doApplyRowFilter();

    grid.applyColumnOption(savedOptions[ix]);
  }

  handleSaveStatus = (name) => {
    this.setState({ inputOn:false, menuShown:false });

    // Grid에서 현재 상태 정보를 가져와야 함. 어떻게?
    // 컬럼 너비, 필터링 정보
    const grid = getCurrentActiveGrid();
    name = name.trim();

    if( isundef(name) || name === '' || isundef(grid) ) {
      this.showInstanceMessage('can not save the status.');
      return;
    }

    const { dataSource } = grid.props;
    const { columnWidth, fixedColumn } = grid.state;
    const ds = dataSource;

    const itemSelected = [];
    for(let c = 0; c < ds.getColumnCount(); ++c) {
      const fd = ds.getColumnFilterData(c);

      if( isundef(fd) ) {
        itemSelected.push(null);
      } else {
        itemSelected.push( fd.filter((d) => d.selected).reduce((m, d) => ({...m, [d.title]:true}), {}) );
      }
    }

    let { savedOptions } = this.state;

    const st = {
      name,
      itemSelected,
      columnWidth,
      fixedColumn
    };

    // console.log('handleSaveStatus', name, st);

    let oldIdx = -1;
    for(let i = 0; oldIdx === -1 && i < savedOptions.length; ++i) {
      oldIdx = name === savedOptions[i].name ? i : -1;
    }

    if( oldIdx !== -1 ) {
      savedOptions[oldIdx] = st;
    } else {
      if( savedOptions.length > 5 ) {
        savedOptions = savedOptions.slice(1);
      }
      savedOptions.push(st);
    }

    localStorage.setItem('options', JSON.stringify(savedOptions));

    this.setState({ savedOptions:savedOptions });
  }

  render() {
    const { waiting, pageType, message, menuShown, urlLog, dataKey, dataMeta, category, lineColorMap, inputOn, savedOptions } = this.state;

    const toastOn = isvalid(message);
    const viewerOn = pageType === 'list';

    const mainMenuClass = cn({ 'menuItem':viewerOn, 'menuItemDisabled':!viewerOn });
    const optMenuClass = cn({ 'menuItem':viewerOn, 'menuItemDisabled':!viewerOn, 'menuNoBorder':true });

    return (
      <div className="mainWrap">
        <div className="mainHeader">
          { <div className="mainMenuButton" onClick={this.handleMenu}><BsList size="28" color="#ffffff" /></div> }
          <div className="mainTitle">{'PM Log Viewer'}</div>
          { viewerOn &&
            <div className="mainMenuButton">
              <a target="_blank" href={`${_serverBaseUrl_}/download/${dataKey}.log`} rel="noopener noreferrer"><RiFileDownloadLine size="24" color="#ffffff"/></a>
            </div>
          }
          { viewerOn && <div className="mainMenuButton" onClick={this.handleClickMenu('entry')}><RiArrowGoBackFill size="24" color="#ffffff"/></div> }
        </div>
        <div className="scrollLock">
          <div
            className={(pageType === 'entry' || pageType === 'edit') ? "mainBodyLimit" : "mainBody"}
            onClick={ () => this.setState({ menuShown:false }) }
          >
            { pageType === 'entry' && <TextInputView controlId="formPmLogId" title="Black Box URL" tip="enter Black Box URL" value={urlLog} onChange={this.handleUrlChanged} /> }
            { pageType === 'entry' && <div className="mainDesc">It will take 10 or more seconds depending on data size and network.</div> }
            { viewerOn && <LogViewer dataKey={dataKey} dataMeta={dataMeta} category={category} lineColorMap={lineColorMap} /> }
          </div>
        </div>
        { waiting &&
          <div className="blockedLayer">
            <Spinner className="spinnerBox" animation="border" variant="primary" />
          </div>
        }
        { toastOn &&
          <div className="blockedLayer" onClick={this.hideToastShow}>
            <Toast className="toastBox" onClose={this.hideToastShow} show={toastOn} delay={3000} autohide animation>
              <Toast.Header>
                <strong className="mr-auto">Message</strong>
              </Toast.Header>
              <Toast.Body>{message}</Toast.Body>
            </Toast>
          </div>
        }
        { inputOn &&
          <div className="inputOverlay" onClick={this.handleClickMenu('close')}>
            <TextInputView controlId="statusName" title="Status Name" tip="enter the status name you want" value={''} onChange={this.handleSaveStatus} />
          </div>
        }
        { menuShown &&
          <div className="overlayLayer" onClick={this.handleClickMenu('close')}>&nbsp;</div>
        }
        { menuShown &&
          <div
            className="sideMenuWrap"
            style={{ left:(menuShown ? '0px' : '-290px') }}
            onClick={this.handleClickMenu('close')}
          >
            {
              <div className="menuBox">
                <div className={cn({ 'menuItem':true, 'menuItemDisabled': (pageType === 'entry') })} onClick={this.handleClickMenu('entry')}>Home</div>
                <div className={mainMenuClass} onClick={this.handleClickMenu('save')}>Save Current Status</div>
                { savedOptions.map((o, ix) => <div key={`ap-opt-${ix}`} className={optMenuClass} onClick={this.handleApplyStatus(ix)}>{`apply [${o.name}]`}</div>) }
              </div>
            }
          </div>
        }
      </div>
    );
  }
}

export default MainFrame;
