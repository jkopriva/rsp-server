export interface LaunchCommandRequest {
    mode: string;
    params: ServerAttributes;
}

export interface ServerAttributes {
    serverType: string;
    id: string;
    attributes: { [index: string]: any };
}