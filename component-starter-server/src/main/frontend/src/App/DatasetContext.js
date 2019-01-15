import React from 'react';


const DatasetContext = React.createContext({ datasets: [] });

class Provider extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            datasets: props.value || [],
            datastore: props.datastore,
        };
        this.state.add = (dataset) => {
            this.setState({ datasets: this.state.datasets.concat(dataset) });
        };
    }

    componentWillReceiveProps(nextProps) {
        if (nextProps.value !== this.state.datasets) {
            this.setState(nextProps.value);
        }
    }

    render() {
        return (
            <DatasetContext.Provider value={this.state}>
                {this.props.children}
            </DatasetContext.Provider>
        );
    }
}

export default {
    Provider,
    Consumer: DatasetContext.Consumer,
};