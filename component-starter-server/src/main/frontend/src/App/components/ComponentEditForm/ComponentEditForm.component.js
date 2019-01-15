import React from 'react';
import PropTypes from 'prop-types';
import classnames from 'classnames';

import { Actions, Icon } from '@talend/react-components';

import Input from '../../Component/Input';
import Help from '../../Component/Help';
import Mapper from '../../Generator/Mapper';
import Processor from '../../Generator/Processor';

import theme from './ComponentEditForm.scss';

const TYPE_INPUT = 'Input';
const TYPE_PROCESSOR = 'Processor';

function onComponentNameChange(service, component) {
	return newName => {
		component.name = newName;
		service.updateComponent();
	};
}

function ComponentEditForm(props) {
	const typeActions = [
		{
			label: 'Input',
			type: TYPE_INPUT,
			className: classnames({
				'btn-info': props.component.type === TYPE_INPUT,
				'btn-inverse': props.component.type !== TYPE_INPUT,
			}),
			onClick: () => {
				props.service.setComponentType(props.component, TYPE_INPUT);
			},
		},
		{
			label: 'Processor/Output',
			type: TYPE_PROCESSOR,
			className: classnames({
				'btn-info': props.component.type === TYPE_PROCESSOR,
				'btn-inverse': props.component.type !== TYPE_PROCESSOR,
			}),
			onClick: () => {
				props.service.setComponentType(props.component, TYPE_PROCESSOR);
			},
		},
	];
	return (
		<div>
			<div className={theme['form-row']}>
				<p className={theme.title}>
					<em>{props.component.configuration.name || ''}</em> Configuration
				</p>
				<div>
					<Actions actions={typeActions} />
					<Help
						title="Component Type"
						i18nKey="component_type"
						content={
							<div>
								<p>Talend Component Kit supports two types of components:</p>
								<ul>
									<li>
										Input: it is a component creating records from itself. It only supports to
										create a main output branch of records.
									</li>
									<li>
										Processor: this component type can read from 1 or multiple inputs the data,
										process them and create 0 or multiple outputs.
									</li>
								</ul>
							</div>
						}
					/>
				</div>
			</div>

			<div className={theme['form-row']}>
				<p className={theme.title}>Configuration</p>
				<form noValidate submit={e => e.preventDefault()}>
					<div className="field">
						<label htmlFor="componentName">Name</label>
						<Help
							title="Component Name"
							i18nKey="component_name"
							content={
								<div>
									<p>Each component has a name which must be unique into a family.</p>
									<p>
										<Icon name="talend-info-circle" /> The name must be a valid java name (no space,
										special characters, ...).
									</p>
								</div>
							}
						/>
						<Input
							className="form-control"
							id="componentName"
							type="text"
							placeholder="Enter the component name..."
							required="required"
							minLength="1"
							onChange={onComponentNameChange(props.service, props.component)}
							aggregate={props.component.configuration}
							accessor="name"
						/>
					</div>
				</form>
			</div>
			{props.component.type === TYPE_INPUT && <Mapper component={props.component} />}
			{props.component.type === TYPE_PROCESSOR && (
				<Processor
					component={props.component}
					addInput={props.service.addInput}
					addOutput={props.service.addOutput}
				/>
			)}
		</div>
	);
}

ComponentEditForm.displayName = 'ComponentEditForm';
ComponentEditForm.propTypes = {
	component: PropTypes.shape({
		name: PropTypes.string,
		type: PropTypes.oneOf([TYPE_INPUT, TYPE_PROCESSOR]),
		configuration: PropTypes.object,
	}),
	service: PropTypes.shape({
		addInput: PropTypes.func,
		addOutput: PropTypes.func,
		setComponentType: PropTypes.func,
	}),
};

export default ComponentEditForm;
