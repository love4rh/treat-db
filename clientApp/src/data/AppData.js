
const AppData = {
    _dbIdx: 0,

    setDatabase: (idx, metaData) => {
        AppData._dbIdx = idx;

        // metaData
    },

    getDatabase: () => {
        return AppData._dbIdx;
    }
};

export default AppData;
export { AppData };
