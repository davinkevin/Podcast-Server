/**
 * Created by kevin on 16/10/2016.
 */

/* Entity */


export declare interface Cover {
  id: string;
  url: string;
  width: number;
  height: number;
}

export declare interface Item {
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

export declare interface Tag {
  id: string;
  name: string;
}

export declare interface Page<T> {
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

export declare interface Sort {
  direction: Direction;
  property: string;
}

export declare enum Direction {
  ASC, DESC
}

declare enum Status {
  NOT_DOWNLOADED,
  DELETED,
  STARTED,
  FINISH,
  STOPPED,
  PAUSED
}

export declare interface SearchItemPageRequest {
  page: number;
  size: number;
  downloaded: boolean;
  tags?: Array<Tag>;
  term?: string;
  orders?: Array<Sort>;
}
