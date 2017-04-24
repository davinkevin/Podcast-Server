/**
* Created by kevin on 07/11/2015 for Podcast Server
*/

import angular from 'angular';

export function Module({name, modules = []}) {
    return Target => {

        Target.$angularModule = angular.module(name, modules.map(extractAngularModuleName));

        if (Target.component) Target.$angularModule.component(Target.$componentName, Target.component);
        if (Target.directive) Target.$angularModule.directive(Target.$directiveName, Target.directive);
        if (Target.routeConfig) Target.$angularModule.config(($routeProvider) => { "ngInject"; $routeProvider.when(...Target.routeConfig); });
        if (Target.$serviceName) Target.$angularModule.service(Target.$serviceName, Target);

        (Target.$config || []).forEach(c => Target.$angularModule.config(c));
        (Target.$constant || []).forEach(c => Target.$angularModule.constant(c.name, c.value));
        (Target.$run || []).forEach(r => Target.$angularModule.run(r));
    };
}

export function RouteConfig({ path, as = 'vm', reloadOnSearch = true, resolve = {}, template = ''}) {
    return Target => {
        if (!path) throw new TypeError("Path should be Defined");

        var parameters = {
            template: template,
            controller: Target,
            controllerAs : as,
            reloadOnSearch : reloadOnSearch,
            resolve : resolve
        };

        Target.$hotKeys && (parameters.hotkeys = Target.$hotKeys);

        Target.routeConfig = [path, parameters];
    };
}

function bindingsForRouteComponent(resolve) {
    let bindings = {};

    if (resolve == {} || resolve == undefined || resolve == null) { return bindings; }

    Object.keys(resolve).forEach(v => bindings[v] = '<');

    return bindings;
}

export function Component({as = '$ctrl', bindings = {}, selector, template = '', transclude = false, path, resolve, reloadOnSearch}) {
    return Target => {
        if (!selector) throw new TypeError("A selector should be defined in the current annotation @Component");

        Target.$componentName = snakeCaseToKebabCase(selector);
        Target.component = {
            transclude : transclude,
            template: template,
            controller : Target,
            controllerAs : as,
            bindings : bindings
        };

        if (path) {
            RouteConfig({path, resolve, reloadOnSearch : reloadOnSearch, template : templateForRouteComponent(selector, resolve)})(Target);
            Target.component.bindings = bindingsForRouteComponent(resolve);
        }
    };
}

export function Directive({scope = true, as = 'vm', bindToController = true, selector, require = ''}) {
    return Target => {
        if (!selector) throw new TypeError("A selector should be defined in the current annotation @Component");

        Target.$directiveName = snakeCaseToKebabCase(selector);
        Target.directive = () => {
            let ddo = {
                restrict : 'A',
                require : require,
                scope : scope,
                controller : Target,
                controllerAs : as,
                bindToController : bindToController
            };

            Target.link && (ddo.link = Target.link);
            return ddo;
        };
    };
}

export function View({template}) {
    return Target => {
        Target.$template = template;
    };
}

export function Service(name) {
    return Target => {
        Target.$serviceName = name;
    };
}

export function Config(configFunction) {
    return Target => {
        if (!Target.$config) Target.$config = [];
        Target.$config.push(configFunction);
    };
}

export function Run(runFunction) {
    return Target => {
        if (!Target.$run) Target.$run = [];
        Target.$run.push(runFunction);
    };
}

export function Boot({ element = document, strictDi = false}) {
    return Target => {
        if (!angular.isDefined(Target.$angularModule))
            throw new TypeError ("@Boot should be used only on a @Module Class");

        angular.element(document).ready(() =>  angular.bootstrap(element, [ Target.$angularModule.name ], { strictDi: strictDi }));
    };
}

export function Constant({ name, value}) {
    return Target => {

        if (!angular.isDefined(name) || !angular.isDefined(value))
            throw new TypeError ("Name and value should be defined for @Constant");

        if (!Target.$constant) Target.$constant = [];
        Target.$constant.push({name : name, value : value});
    };
}

export function UibModal({animation = true, backdrop = true, bindToController = true, as = 'vm', keyboard = true, resolve, size, template = ''}) {
    return Target => {
        let $UibModalConf = {
            animation : animation,
            backdrop : backdrop,
            bindToController: bindToController,
            controller : Target,
            controllerAs : as,
            keyboard : keyboard,
            resolve : resolve,
            size : size,
            template : template
        };

        Target.$UibModalConf = {
            asDefault : () => $UibModalConf,
            withResolve : (resolve) => angular.extend({}, $UibModalConf, {resolve : angular.extend({}, $UibModalConf.resolve, resolve) })
        };
    };
}

function templateForRouteComponent($componentName, resolve) {
    if (resolve == {} || resolve == undefined || resolve == null) { return `<${$componentName}></${$componentName}>`; }

    let attributes = Object.keys(resolve).map(k => `${kebabToSnakeCase(k)}="$resolve.${k}" `).join("");
    return `<${$componentName} ${attributes}></${$componentName}>`;
}

function snakeCaseToKebabCase(string) {
    return string.replace( /-([a-z])/ig, (_,letter) => letter.toUpperCase());
}

function kebabToSnakeCase(string) {
    return string.replace(/(.)([A-Z]+)/g, (m, previous, uppers) => previous + '-' + uppers.toLowerCase().split('').join('-'));
}

function extractAngularModuleName(clazz) {
    if (clazz.$angularModule)
        return clazz.$angularModule.name;

    return clazz.name ? clazz.name : clazz;
}