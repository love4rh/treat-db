import React, { Component } from 'react';

import { isundef, isvalid, setGlobalMessageHandle } from '../util/tool.js';
import { apiProxy } from '../util/apiProxy.js';
import { Log } from '../util/Logging.js';

import { BsList } from 'react-icons/bs';
import Spinner from 'react-bootstrap/Spinner'
import Toast from 'react-bootstrap/Toast'

import UserSelector from '../view/UserSelector.js';
import SQLFrame from '../view/SQLFrame.js';

import mock from '../mock/db.json';

import './MainFrame.scss';


const debugOn = false;


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

    apiProxy.signOut();
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

  refreshMetaData = () => {
    apiProxy.getMetaData(
      (res) => {
        // console.log('metadata result:', res.data.response);
        this.setState({ pageType:'main', databases:res.response });
      },
      (err) => {
        console.log('error:', err);
        Log.w(err);
        this.showInstanceMessage('error occurrs.');
      }
    );
  }

  handleChangeUser = (userID, password) => {
    if( isundef(userID) ) {
      this.showInstanceMessage('invalid indentifier.');
      return;
    }

    userID = userID.trim();
    if( userID === '' ) {
      this.showInstanceMessage('invalid indentifier: [' + userID + ']');
      return;
    }

    localStorage.setItem('lastUser', userID);

    if( debugOn ) {
      Log.i('signed in with ' + userID);
      this.setState({ pageType:'main', databases:mock });
      return;
    }

    apiProxy.signIn(userID, password,
      (res) => {
        Log.i('signed in with ' + userID);
        this.refreshMetaData();
      },
      (err) => {
        console.log(err);
        this.showInstanceMessage('error occurs');
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

    return (
      <div className="mainWrap">
        <div className="mainHeader">
          { <div className="mainMenuButton" onClick={this.handleMenu}><BsList size="28" color="#ffffff" /></div> }
          <div className="mainTitle">{this.props.appTitle}</div>
        </div>
        <div className="scrollLock">
          { pageType === 'entry' && <UserSelector userID={userID} onChangeUser={this.handleChangeUser} /> }
          { pageType === 'main' && <SQLFrame databases={databases} /> }
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
