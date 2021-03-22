import React, { Component } from 'react';

import cn from 'classnames';

import { isundef, isvalid, istrue, nvl, setGlobalMessageHandle } from '../util/tool.js';

import { getCurrentActiveGrid } from '../grid/common.js';

import { apiProxy, _serverBaseUrl_ } from '../util/apiProxy.js';

import { BsList } from 'react-icons/bs';
import { RiArrowGoBackFill, RiFileDownloadLine } from 'react-icons/ri';

import Spinner from 'react-bootstrap/Spinner'
import Toast from 'react-bootstrap/Toast'

import UserSelector from '../view/UserSelector.js';
import SQLFrame from '../view/SQLFrame.js';

import './MainFrame.scss';



class MainFrame extends Component {
  constructor (props) {
    super(props);

    const lastUser = localStorage.getItem('lastUser') || '';

    this.state = {
      pageType: 'entry', // entry, main
      userID: lastUser,
      message: null,
      waiting: false,
      menuShown: false,
      redrawCount: 0,
      databases: null
    };

    this.handleUnload = this.handleUnload.bind(this);
  }

  componentDidMount() {
    document.title = this.props.appTitle;

    window.addEventListener('beforeunload', this.handleUnload);
    setGlobalMessageHandle(this.showInstanceMessage);
    apiProxy.setWaitHandle(this.enterWaiting, this.leaveWaiting);
  }

  componentWillUnmount() {
    window.removeEventListener('beforeunload', this.handleUnload);
  }

  handleUnload = (ev) => {
    /*
    const message = 'Are you sure you want to close?';

    ev.preventDefault();
    (ev || window.event).returnValue = message;

    return message;
    // */
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

  handleChangeUser = (userID, authCode) => {
    if( isundef(userID) ) {
      this.showInstanceMessage('invalid indentifier.');
      return;
    }

    userID = userID.trim();
    if( userID === '' ) {
      this.showInstanceMessage('invalid indentifier: [' + userID + ']');
      return;
    }

    // TODO 인증

    /* // authCode 서버관리
    if( authCode !== '1234') {
      this.showInstanceMessage('Invalid Authorized Code');
      return;
    }
    // */

    // console.log('main', 'onChangeUser', userID);
    localStorage.setItem('lastUser', userID);

    // TODO 초기 데이터 로딩
    apiProxy.getMetaData(
      '124816',
      (res) => {
        // console.log('metadata result:', res);
        this.setState({ pageType:'main', databases:res.data.response });
      },
      (err) => {
        console.log('error:', err);
        this.showInstanceMessage('error occurrs.');
      }
    );
  }

  handleMenu = () => {
    const { menuShown } = this.state;
    this.setState({ menuShown: !menuShown });
  }

  hideToastShow = () => {
    this.setState({ message: null });
  }

  render() {
    const { userID, waiting, pageType, message, menuShown, databases } = this.state;

    const toastOn = isvalid(message);
    const viewerOn = pageType === 'main';

    return (
      <div className="mainWrap">
        <div className="mainHeader">
          { <div className="mainMenuButton" onClick={this.handleMenu}><BsList size="28" color="#ffffff" /></div> }
          <div className="mainTitle">{this.props.appTitle}</div>
        </div>
        <div className="scrollLock">
          { pageType == 'entry' && <UserSelector userID={userID} onChangeUser={this.handleChangeUser} /> }
          { pageType == 'main' && <SQLFrame databases={databases} /> }
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
        { menuShown &&
          <div className="overlayLayer" onClick={this.handleClickMenu('close')}>&nbsp;</div>
        }
      </div>
    );
  }
}

export default MainFrame;
