
const AppData = {
    _dbIdx: 0,

    setDatabase: (idx) => {
        AppData._dbIdx = idx;
    },

    getDatabase: () => {
        return AppData._dbIdx;
    }
};

export default AppData;
export { AppData };
