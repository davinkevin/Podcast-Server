/**
 * Created by kevin on 16/10/2016.
 */

/* Entity */
export interface Cover {
  id: string;
  url: string;
  width: number;
  height: number;
}

export interface Item {
  cover: Cover;
  creationDate: string;
  id: string;
  isDownloaded: boolean;
  mimeType: string;
  podcastId: string;
  proxyURL: string;
  status: Status;
  title: string;
  url: string;
}

export interface Tag {
  id: string;
  name: string;
}

export interface Page<T> {
  content: Array<T>;
  first: boolean;
  last: boolean;
  totalPages: number;
  totalElements: number;
  numberOfElements: number;
  size: number;
  number: number;
  sort: Array<Sort>;
}

export interface Sort {
  direction: Direction;
  property: string;
}

export enum Direction {
  ASC = <any>'ASC',
  DESC = <any>'DESC'
}

export enum Status {
  NOT_DOWNLOADED = <any>'NOT_DOWNLOADED',
  DELETED = <any>'DELETED',
  STARTED = <any>'STARTED',
  FINISH = <any>'FINISH',
  STOPPED = <any>'STOPPED',
  PAUSED = <any>'PAUSED'
}

export interface SearchItemPageRequest {
  page: number;
  size: number;
  downloaded: boolean;
  tags?: Array<Tag>;
  term?: string;
  sort?: Array<Sort>;
}
